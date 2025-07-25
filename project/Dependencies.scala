import sbt._

object Dependencies {
  val circeVersion = "0.14.14"
  private val keycloakVersion = "26.2.5"

  lazy val authUtils = "uk.gov.nationalarchives" %% "tdr-auth-utils" % "0.0.251"
  lazy val awsLambda = "com.amazonaws" % "aws-lambda-java-core" % "1.3.0"
  lazy val awsSsm = "software.amazon.awssdk" % "ssm" % "2.32.4"
  lazy val typeSafeConfig = "com.typesafe" % "config" % "1.4.4"
  lazy val graphqlClient = "uk.gov.nationalarchives" %% "tdr-graphql-client" % "0.0.242"
  lazy val generatedGraphql = "uk.gov.nationalarchives" %% "tdr-generated-graphql" % "0.0.423"
  lazy val circeCore = "io.circe" %% "circe-core" % circeVersion
  lazy val circeParser = "io.circe" %% "circe-parser" % circeVersion
  lazy val keycloakCore = "org.keycloak" % "keycloak-core" % keycloakVersion
  lazy val keycloakAdminClient =  "org.keycloak" % "keycloak-admin-client" % keycloakVersion
  lazy val mockito = "org.mockito" %% "mockito-scala" % "2.0.0"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.19"
  lazy val wiremockStandalone = "com.github.tomakehurst" % "wiremock-standalone" % "3.0.1"
}
