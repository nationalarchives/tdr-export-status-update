import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import com.typesafe.config.{Config, ConfigFactory}
import graphql.codegen.UpdateConsignmentStatus.updateConsignmentStatus.Data
import graphql.codegen.UpdateConsignmentStatus.{updateConsignmentStatus => ucs}
import graphql.codegen.types.ConsignmentStatusInput
import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers._
import sangria.ast.Document
import sttp.client3.{HttpError, HttpURLConnectionBackend, Identity, Response, SttpBackend}
import sttp.model.StatusCode
import uk.gov.nationalarchives.exportstatusupdate.GraphQlApi
import uk.gov.nationalarchives.tdr.GraphQLClient.Extensions
import uk.gov.nationalarchives.tdr.error.{GraphQlError, HttpException}
import uk.gov.nationalarchives.tdr.keycloak.{KeycloakUtils, TdrKeycloakDeployment}
import uk.gov.nationalarchives.tdr.{GraphQLClient, GraphQlResponse}
import utils.ExternalServicesTestUtils

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.ClassTag

class GraphQlApiTest extends ExternalServicesTestUtils with MockitoSugar with EitherValues {

  val configFactory: Config = ConfigFactory.load

  val clientSecret: String = configFactory.getString("auth.clientSecret")
  val authUrl: String = configFactory.getString("auth.url")
  val timeToLiveInSeconds: Int = 3600

  implicit val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()
  implicit val tdrKeycloakDeployment: TdrKeycloakDeployment = TdrKeycloakDeployment(authUrl, "tdr", timeToLiveInSeconds)

  "The updateExportStatus method" should "request a service account token" in {
    val keycloakUtils = mock[KeycloakUtils]
    val client = mock[GraphQLClient[ucs.Data, ucs.Variables]]
    val graphQlApi = GraphQlApi(keycloakUtils, client)

    val consignmentId = UUID.fromString("c2efd3e6-6664-4582-8c28-dcf891f60e68")
    val statusValue = "Failed"

    when(
      keycloakUtils.serviceAccountToken[Identity](any[String], any[String])(
        any[SttpBackend[Identity, Any]], any[ClassTag[Identity[_]]], any[TdrKeycloakDeployment]
      )
    ).thenReturn(
      Future.successful(new BearerAccessToken("token"))
    )
    when(
      client.getResult[Identity](any[BearerAccessToken], any[Document], any[Option[ucs.Variables]])(
        any[SttpBackend[Identity, Any]], any[ClassTag[Identity[_]]]
      )
    ).thenReturn(
      Future.successful(
        GraphQlResponse(
          Some(Data(Some(1))),
          List()
        )
      )
    )

    graphQlApi.updateExportStatus(consignmentId, statusValue, clientSecret).futureValue

    val expectedId = "tdr-backend-checks"
    val expectedSecret = "c2VjcmV0"

    verify(keycloakUtils).serviceAccountToken(expectedId, expectedSecret)
  }

  "The updateExportStatus method" should "call the graphql api with the correct data" in {
    val keycloakUtils = mock[KeycloakUtils]
    val client = mock[GraphQLClient[ucs.Data, ucs.Variables]]
    val graphQlApi = GraphQlApi(keycloakUtils, client)
    val document = ucs.document

    val consignmentId = UUID.fromString("c2efd3e6-6664-4582-8c28-dcf891f60e68")
    val statusValue = "Failed"
    val variables = ucs.Variables(ConsignmentStatusInput(consignmentId, "Export", statusValue))

    when(
      keycloakUtils.serviceAccountToken[Identity](any[String], any[String])(
        any[SttpBackend[Identity, Any]], any[ClassTag[Identity[_]]], any[TdrKeycloakDeployment]
      )
    ).thenReturn(Future.successful(new BearerAccessToken("token")))
    when(client.getResult[Identity](any[BearerAccessToken], any[Document], any[Option[ucs.Variables]])(any[SttpBackend[Identity, Any]], any[ClassTag[Identity[_]]]))
      .thenReturn(
        Future.successful(
          GraphQlResponse(
            Some(ucs.Data(Some(1))),
            List()
          )
        )
      )
    graphQlApi.updateExportStatus(consignmentId, statusValue, clientSecret).futureValue

    verify(client).getResult[Identity](new BearerAccessToken("token"), document, Some(variables))
  }

  "The updateExportStatus method" should "error if the auth server is unavailable" in {
    val client = mock[GraphQLClient[ucs.Data, ucs.Variables]]
    val keycloakUtils = mock[KeycloakUtils]
    val consignmentId = UUID.fromString("c2efd3e6-6664-4582-8c28-dcf891f60e68")
    val statusValue = "Failed"

    when(keycloakUtils.serviceAccountToken[Identity](any[String], any[String])(
      any[SttpBackend[Identity, Any]], any[ClassTag[Identity[_]]], any[TdrKeycloakDeployment]))
      .thenThrow(HttpError("An error occurred contacting the auth server", StatusCode.InternalServerError))

    val exception = intercept[HttpError[String]] {
      GraphQlApi(keycloakUtils, client).updateExportStatus(consignmentId, statusValue, clientSecret).futureValue
    }
    exception.body should equal("An error occurred contacting the auth server")
  }

  "The updateExportStatus method" should "error if the graphql server is unavailable" in {

    val client = mock[GraphQLClient[ucs.Data, ucs.Variables]]
    val keycloakUtils = mock[KeycloakUtils]
    val consignmentId = UUID.fromString("c2efd3e6-6664-4582-8c28-dcf891f60e68")
    val statusValue = "Failed"

    val body: Either[String, String] = Left("Graphql error")

    val response = Response(body, StatusCode.ServiceUnavailable)

    when(keycloakUtils.serviceAccountToken[Identity](any[String], any[String])(
      any[SttpBackend[Identity, Any]], any[ClassTag[Identity[_]]], any[TdrKeycloakDeployment]))
      .thenReturn(Future.successful(new BearerAccessToken("token")))
    when(client.getResult[Identity](any[BearerAccessToken], any[Document], any[Option[ucs.Variables]])(any[SttpBackend[Identity, Any]], any[ClassTag[Identity[_]]])).thenThrow(new HttpException(response))

    val res = GraphQlApi(keycloakUtils, client).updateExportStatus(
      consignmentId,
      statusValue,
      clientSecret
    ).failed.futureValue

    res.getMessage shouldEqual "Unexpected response from GraphQL API: Response(Left(Graphql error),503,,List(),List(),RequestMetadata(GET,http://example.com,List()))"
  }

  "The updateExportStatus method" should "error if the graphql query returns not authorised errors" in {
    val client = mock[GraphQLClient[ucs.Data, ucs.Variables]]
    val keycloakUtils = mock[KeycloakUtils]
    val consignmentId = UUID.fromString("c2efd3e6-6664-4582-8c28-dcf891f60e68")
    val statusValue = "Failed"

    when(keycloakUtils.serviceAccountToken[Identity](any[String], any[String])(
      any[SttpBackend[Identity, Any]], any[ClassTag[Identity[_]]], any[TdrKeycloakDeployment]))
      .thenReturn(Future.successful(new BearerAccessToken("token")))
    val graphqlResponse: GraphQlResponse[Data] =
      GraphQlResponse(
        Option.empty,
        List(
          GraphQlError(
            GraphQLClient.Error(
              "Not authorised message",
              List(),
              List(),
              Some(
                Extensions(Some("NOT_AUTHORISED"))
              )
            )
          )
        )
      )
    when(client.getResult[Identity](any[BearerAccessToken], any[Document], any[Option[ucs.Variables]])(any[SttpBackend[Identity, Any]], any[ClassTag[Identity[_]]]))
      .thenReturn(Future.successful(graphqlResponse))

    val res = GraphQlApi(keycloakUtils, client).updateExportStatus(
      consignmentId,
      statusValue,
      clientSecret
    ).failed.futureValue

    res.getMessage should include("Not authorised message")
  }

  "The updateExportStatus method" should "error if the graphql query returns a general error" in {
    val client = mock[GraphQLClient[ucs.Data, ucs.Variables]]
    val keycloakUtils = mock[KeycloakUtils]
    val consignmentId = UUID.fromString("c2efd3e6-6664-4582-8c28-dcf891f60e68")
    val statusValue = "Failed"

    when(keycloakUtils.serviceAccountToken[Identity](any[String], any[String])(
      any[SttpBackend[Identity, Any]], any[ClassTag[Identity[_]]], any[TdrKeycloakDeployment]))
      .thenReturn(Future.successful(new BearerAccessToken("token")))
    val graphqlResponse: GraphQlResponse[ucs.Data] =
      GraphQlResponse(
        Option.empty,
        List(
          GraphQlError(
            GraphQLClient.Error(
              "General error",
              List(),
              List(),
              Option.empty
            )
          )
        )
      )
    when(client.getResult[Identity](any[BearerAccessToken], any[Document], any[Option[ucs.Variables]])(any[SttpBackend[Identity, Any]], any[ClassTag[Identity[_]]]))
      .thenReturn(Future.successful(graphqlResponse))

    val res = GraphQlApi(keycloakUtils, client).updateExportStatus(
      consignmentId,
      statusValue,
      clientSecret
    ).failed.futureValue
    res.getMessage shouldEqual "GraphQL response contained errors: General error"
  }
}
