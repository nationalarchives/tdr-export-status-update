package uk.gov.nationalarchives.exportstatusupdate

import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import com.typesafe.config.{ConfigFactory, Config => TypeSafeConfig}
import graphql.codegen.UpdateConsignmentStatus.{updateConsignmentStatus => ucs}
import io.circe
import io.circe.generic.auto._
import io.circe.parser.decode
import sttp.client3.{HttpURLConnectionBackend, Identity, SttpBackend}
import uk.gov.nationalarchives.aws.utils.Clients.kms
import uk.gov.nationalarchives.aws.utils.KMSUtils
import uk.gov.nationalarchives.exportstatusupdate.Lambda.LambdaInput
import uk.gov.nationalarchives.tdr.GraphQLClient
import uk.gov.nationalarchives.tdr.keycloak.{KeycloakUtils, TdrKeycloakDeployment}

import java.io.{InputStream, OutputStream}
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.io.Source

class Lambda extends RequestStreamHandler {
  val configFactory: TypeSafeConfig = ConfigFactory.load
  implicit val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()
  implicit val keycloakDeployment: TdrKeycloakDeployment = TdrKeycloakDeployment(
    configFactory.getString("auth.url"),
    "tdr",
    60
  )

  val keycloakUtils = new KeycloakUtils()
  val apiUrl: String = configFactory.getString("api.url")
  val updateConsignmentStatusClient = new GraphQLClient[ucs.Data, ucs.Variables](apiUrl)
  val graphQlApi: GraphQlApi = GraphQlApi(keycloakUtils, updateConsignmentStatusClient)

  override def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit = {
    val rawInput: String = Source.fromInputStream(input).mkString
    val result: Either[circe.Error, LambdaInput] = decode[LambdaInput](rawInput)

    val response: Future[ucs.Data] = result match {
      case Left(error) => throw new RuntimeException(error)
      case Right(lambdaInput) =>
        val decryptedSecret: String = decryptSecret()
        val consignmentIdAsUuid = lambdaInput.consignmentId
        setStatusToFailed(consignmentIdAsUuid, decryptedSecret)
    }
    Await.result(response, 10.seconds)
  }

  private def decryptSecret(): String = {
    val encryptionContext = Map("LambdaFunctionName" -> configFactory.getString("functionName"))
    val kmsUtils = KMSUtils(kms(configFactory.getString("kms.endpoint")), encryptionContext)
    kmsUtils.decryptValue(configFactory.getString("auth.clientSecret"))
  }

  private def setStatusToFailed(consignmentId: UUID, secret: String): Future[ucs.Data] = {
    graphQlApi.updateExportStatus(consignmentId, "Failed", secret)
  }
}

object Lambda {
  case class LambdaInput(consignmentId: UUID)
}
