import com.github.tomakehurst.wiremock.client.WireMock.{equalToJson, postRequestedFor, urlEqualTo}
import com.nimbusds.oauth2.sdk.token.BearerAccessToken
import graphql.codegen.UpdateConsignmentStatus.updateConsignmentStatus.Data
import graphql.codegen.UpdateConsignmentStatus.{updateConsignmentStatus => ucs}
import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers._
import sangria.ast.Document
import sttp.client3.{Identity, SttpBackend}
import uk.gov.nationalarchives.exportstatusupdate.{GraphQlApi, Lambda}
import uk.gov.nationalarchives.tdr.keycloak.{KeycloakUtils, TdrKeycloakDeployment}
import uk.gov.nationalarchives.tdr.{GraphQLClient, GraphQlResponse}
import utils.ExternalServicesTestUtils

import java.io.ByteArrayOutputStream
import java.util.UUID
import scala.concurrent.Future
import scala.reflect.ClassTag

class LambdaTest extends ExternalServicesTestUtils with MockitoSugar with EitherValues {

  class LambdaMock(mockUcsClient: GraphQLClient[Data, ucs.Variables], mockKeycloakUtils: KeycloakUtils) extends Lambda {
    override val authUrl: String = configFactory.getString("auth.url")
    override val timeToLiveInSecs: Int = 3600
    implicit val tdrKeycloakDeployment: TdrKeycloakDeployment = TdrKeycloakDeployment(authUrl, "tdr", timeToLiveInSecs)
    override val updateConsignmentStatusClient: GraphQLClient[Data, ucs.Variables] = mockUcsClient
    override val keycloakUtils: KeycloakUtils = mockKeycloakUtils
    override val graphQlApi: GraphQlApi = GraphQlApi(keycloakUtils, updateConsignmentStatusClient)
  }

  "Creating the lambda class" should "call systems manager with the correct arguments" in {
    val consignmentId = UUID.randomUUID()
    val inputStream = getInputStream(consignmentId.toString)

    val mockUcsClient = mock[GraphQLClient[ucs.Data, ucs.Variables]]
    val mockKeycloakUtils: KeycloakUtils = mock[KeycloakUtils]

    when(
      mockKeycloakUtils.serviceAccountToken[Identity](
        any[String],
        any[String]
      )(
        any[SttpBackend[Identity, Any]],
        any[ClassTag[Identity[_]]],
        any[TdrKeycloakDeployment]
      )
    ).thenReturn(Future.successful(new BearerAccessToken("token")))
    when(
      mockUcsClient.getResult[Identity](
        any[BearerAccessToken],
        any[Document],
        any[Option[ucs.Variables]]
      )(
        any[SttpBackend[Identity, Any]],
        any[ClassTag[Identity[_]]]
      )
    ).thenReturn(
      Future.successful(
        GraphQlResponse(
          Some(ucs.Data(Some(1))),
          List()
        )
      )
    )

    new LambdaMock(mockUcsClient, mockKeycloakUtils)
      .handleRequest(inputStream, mock[ByteArrayOutputStream], null)
    wiremockSsmServer
      .verify(postRequestedFor(urlEqualTo("/"))
        .withRequestBody(equalToJson("""{"Name" : "client/secret/path","WithDecryption" : true}""")))
  }

  "The handleRequest method" should "not throw an exception, given a correctly formatted consignmentId" in {
    val consignmentId = UUID.randomUUID()
    val inputStream = getInputStream(consignmentId.toString)

    val mockUcsClient = mock[GraphQLClient[ucs.Data, ucs.Variables]]
    val mockKeycloakUtils: KeycloakUtils = mock[KeycloakUtils]

    when(
      mockKeycloakUtils.serviceAccountToken[Identity](
        any[String],
        any[String]
      )(
        any[SttpBackend[Identity, Any]],
        any[ClassTag[Identity[_]]],
        any[TdrKeycloakDeployment]
      )
    ).thenReturn(Future.successful(new BearerAccessToken("token")))
    when(
      mockUcsClient.getResult[Identity](
        any[BearerAccessToken],
        any[Document],
        any[Option[ucs.Variables]]
      )(
        any[SttpBackend[Identity, Any]],
        any[ClassTag[Identity[_]]]
      )
    ).thenReturn(
      Future.successful(
        GraphQlResponse(
          Some(ucs.Data(Some(1))),
          List()
        )
      )
    )

    noException should be thrownBy new LambdaMock(mockUcsClient, mockKeycloakUtils)
      .handleRequest(inputStream, mock[ByteArrayOutputStream], null)
  }

  "The handleRequest method" should "throw an exception, given an incorrectly formatted consignmentId" in {
    val inputStream = getInputStream("not a UUID")

    val mockUcsClient = mock[GraphQLClient[ucs.Data, ucs.Variables]]
    val mockKeycloakUtils: KeycloakUtils = mock[KeycloakUtils]

    when(
      mockKeycloakUtils.serviceAccountToken[Identity](any[String], any[String])(
        any[SttpBackend[Identity, Any]],
        any[ClassTag[Identity[_]]],
        any[TdrKeycloakDeployment]
      )
    ).thenReturn(Future.successful(new BearerAccessToken("token")))
    when(
      mockUcsClient.getResult[Identity](
        any[BearerAccessToken],
        any[Document],
        any[Option[ucs.Variables]]
      )(any[SttpBackend[Identity, Any]], any[ClassTag[Identity[_]]])
    ).thenReturn(
      Future.successful(
        GraphQlResponse(
          Some(ucs.Data(Some(1))),
          List()
        )
      )
    )

    val rtException: RuntimeException =
      intercept[RuntimeException] {
        new LambdaMock(mockUcsClient, mockKeycloakUtils)
          .handleRequest(inputStream, mock[ByteArrayOutputStream], null)
      }

    rtException.getMessage should equal(
      """DecodingFailure at .consignmentId: Got value '"not a UUID"' with wrong type, expecting string"""
    )
  }
}
