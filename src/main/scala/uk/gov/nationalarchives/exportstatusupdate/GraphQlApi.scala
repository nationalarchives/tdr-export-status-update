package uk.gov.nationalarchives.exportstatusupdate

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import graphql.codegen.UpdateConsignmentStatus.{updateConsignmentStatus => ucs}
import graphql.codegen.types.ConsignmentStatusInput
import sttp.client3._
import uk.gov.nationalarchives.tdr.error.NotAuthorisedError
import uk.gov.nationalarchives.tdr.keycloak.{KeycloakUtils, TdrKeycloakDeployment}
import uk.gov.nationalarchives.tdr.{GraphQLClient, GraphQlResponse}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class GraphQlApi(val keycloak: KeycloakUtils,
                 updateConsignmentStatusClient: GraphQLClient[ucs.Data, ucs.Variables])(
                 implicit val logger: Logger,
                 keycloakDeployment: TdrKeycloakDeployment,
                 backend: SttpBackend[Identity, Any]) {

  implicit class ErrorUtils[D](response: GraphQlResponse[D]) {
    val errorString: String = response.errors.map(_.message).mkString("\n")
  }

  def updateExportStatus(consignmentId: UUID, statusValue: String, clientSecret: String)(implicit executionContext: ExecutionContext): Future[ucs.Data] = {
    val configFactory = ConfigFactory.load
    val consignmentStatusInput = ConsignmentStatusInput(consignmentId, "Export", Some(statusValue))
    val queryResult: Future[Either[String, GraphQlResponse[ucs.Data]]] = (for {
      token <- keycloak.serviceAccountToken(configFactory.getString("auth.clientId"), clientSecret)
      response <- updateConsignmentStatusClient.getResult(token, ucs.document, Some(ucs.Variables(consignmentStatusInput)))
    } yield Right(response)) recover (e => Left(e.getMessage))

    queryResult.flatMap {
      case Right(response) => response.errors match {
        case Nil =>
          logger.info(s"Export status updated to $statusValue for consignment $consignmentId")
          Future.successful(response.data.get)
        case List(authError: NotAuthorisedError) => Future.failed(new Exception(authError.message))
        case errors => Future.failed(new Exception(s"GraphQL response contained errors: ${errors.map(_.message).mkString}"))
      }
      case Left(error) => Future.failed(new Exception(error))
    }
  }
}

object GraphQlApi {
  def apply(keycloak: KeycloakUtils,
            updateConsignmentStatusClient: GraphQLClient[ucs.Data, ucs.Variables])
           (implicit backend: SttpBackend[Identity, Any],
            keycloakDeployment: TdrKeycloakDeployment): GraphQlApi = {
    val logger: Logger = Logger[GraphQlApi]
    new GraphQlApi(keycloak, updateConsignmentStatusClient)(logger, keycloakDeployment, backend)
  }
}
