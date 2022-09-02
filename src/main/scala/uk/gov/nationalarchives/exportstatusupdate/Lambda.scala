package uk.gov.nationalarchives.exportstatusupdate

import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import com.typesafe.config.{ConfigFactory, Config => TypeSafeConfig}
import graphql.codegen.UpdateConsignmentStatus.{updateConsignmentStatus => ucs}
import io.circe
import io.circe.generic.auto._
import io.circe.parser.decode
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.regions.Region
import sttp.client3.{HttpURLConnectionBackend, Identity, SttpBackend}
import uk.gov.nationalarchives.exportstatusupdate.Lambda.LambdaInput
import uk.gov.nationalarchives.tdr.GraphQLClient
import uk.gov.nationalarchives.tdr.keycloak.{KeycloakUtils, TdrKeycloakDeployment}

import java.io.{InputStream, OutputStream}
import java.net.URI
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.io.Source
import software.amazon.awssdk.services.ssm.SsmClient
import software.amazon.awssdk.services.ssm.model.GetParameterRequest

class Lambda extends RequestStreamHandler {
  val configFactory: TypeSafeConfig = ConfigFactory.load
  val authUrl: String = configFactory.getString("auth.url")
  val apiUrl: String = configFactory.getString("api.url")
  val clientSecretPath: String = configFactory.getString("auth.clientSecretPath")
  val endpoint: String = configFactory.getString("ssm.endpoint")
  val timeToLiveInSecs: Int = 60

  implicit val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()
  implicit val keycloakDeployment: TdrKeycloakDeployment = TdrKeycloakDeployment(
    authUrl,
    "tdr",
    timeToLiveInSecs
  )

  val keycloakUtils = new KeycloakUtils()
  val updateConsignmentStatusClient = new GraphQLClient[ucs.Data, ucs.Variables](apiUrl)
  val graphQlApi: GraphQlApi = GraphQlApi(keycloakUtils, updateConsignmentStatusClient)

  override def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit = {
    val rawInput: String = Source.fromInputStream(input).mkString
    val result: Either[circe.Error, LambdaInput] = decode[LambdaInput](rawInput)

    val response: Future[ucs.Data] = result match {
      case Left(error) => throw new RuntimeException(error)
      case Right(lambdaInput) =>
        val decryptedSecret: String = getClientSecret(clientSecretPath, endpoint)
        val consignmentIdAsUuid = lambdaInput.consignmentId
        setStatusToFailed(consignmentIdAsUuid, decryptedSecret)
    }
    Await.result(response, 10.seconds)
  }

  private def setStatusToFailed(consignmentId: UUID, secret: String): Future[ucs.Data] = {
    graphQlApi.updateExportStatus(consignmentId, "Failed", secret)
  }

  private def getClientSecret(secretPath: String, endpoint: String): String = {
    val httpClient = ApacheHttpClient.builder.build
    val ssmClient: SsmClient = SsmClient.builder()
      .endpointOverride(URI.create(endpoint))
      .httpClient(httpClient)
      .region(Region.EU_WEST_2)
      .build()
    val getParameterRequest = GetParameterRequest.builder.name(secretPath).withDecryption(true).build
    ssmClient.getParameter(getParameterRequest).parameter().value()
  }
}

object Lambda {
  case class LambdaInput(consignmentId: UUID)
}
