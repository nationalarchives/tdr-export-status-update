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
    implicit val tdrKeycloakDeployment: TdrKeycloakDeployment = TdrKeycloakDeployment(configFactory.getString("auth.url"), "tdr", 3600)
    override val updateConsignmentStatusClient: GraphQLClient[Data, ucs.Variables] = mockUcsClient
    override val keycloakUtils: KeycloakUtils = mockKeycloakUtils
    override val graphQlApi: GraphQlApi = GraphQlApi(keycloakUtils, updateConsignmentStatusClient)
  }

  "The handleRequest method" should "doesn't throw an exception, given a correctly formatted consignmentId" in {
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

  "The handleRequest method" should "throws an exception, given an incorrectly formatted consignmentId" in {
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
      """DecodingFailure(Got value '"not a UUID"' with wrong type, expecting string, List(DownField(consignmentId)))"""
    )
  }
}
