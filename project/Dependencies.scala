import sbt._

object Dependencies {
  val circeVersion = "0.14.3"
  private val keycloakVersion = "16.1.0"

  lazy val authUtils = "uk.gov.nationalarchives" %% "tdr-auth-utils" % "0.0.103"
  lazy val awsLambda = "com.amazonaws" % "aws-lambda-java-core" % "1.2.2"
  lazy val awsSsm = "software.amazon.awssdk" % "ssm" % "2.19.5"
  lazy val typeSafeConfig = "com.typesafe" % "config" % "1.4.2"
  lazy val generatedGraphql = "uk.gov.nationalarchives" %% "tdr-generated-graphql" % "0.0.290"
  lazy val graphqlClient = "uk.gov.nationalarchives" %% "tdr-graphql-client" % "0.0.77"
  lazy val circeCore = "io.circe" %% "circe-core" % circeVersion
  lazy val circeParser = "io.circe" %% "circe-parser" % circeVersion
  lazy val keycloakCore = "org.keycloak" % "keycloak-core" % keycloakVersion
  lazy val keycloakAdminClient =  "org.keycloak" % "keycloak-admin-client" % keycloakVersion
  lazy val mockito = "org.mockito" %% "mockito-scala" % "1.17.12"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.14"
  lazy val wiremock = "com.github.tomakehurst" % "wiremock-jre8" % "2.35.0"
}
