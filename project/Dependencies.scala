import sbt._

object Dependencies {
  val circeVersion = "0.14.2"
  private val keycloakVersion = "16.1.0"
  private val log4CatsVersion = "1.1.1"
  val pureConfigVersion = "0.17.1"

  lazy val authUtils = "uk.gov.nationalarchives" %% "tdr-auth-utils" % "0.0.58"
  lazy val awsLambda = "com.amazonaws" % "aws-lambda-java-core" % "1.2.1"
  lazy val awsUtils = "uk.gov.nationalarchives" %% "tdr-aws-utils" % "0.1.31"
  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "3.3.12"
  lazy val typeSafeConfig = "com.typesafe" % "config" % "1.4.2"
  lazy val generatedGraphql = "uk.gov.nationalarchives" %% "tdr-generated-graphql" % "0.0.243"
  lazy val graphqlClient = "uk.gov.nationalarchives" %% "tdr-graphql-client" % "0.0.37"
  lazy val circeCore = "io.circe" %% "circe-core" % circeVersion
  lazy val circeParser = "io.circe" %% "circe-parser" % circeVersion
  lazy val keycloakCore = "org.keycloak" % "keycloak-core" % keycloakVersion
  lazy val keycloakAdminClient =  "org.keycloak" % "keycloak-admin-client" % keycloakVersion
  lazy val log4cats = "io.chrisdavenport" %% "log4cats-core" % log4CatsVersion
  lazy val log4catsSlf4j = "io.chrisdavenport" %% "log4cats-slf4j" % log4CatsVersion
  lazy val mockito = "org.mockito" %% "mockito-scala" % "1.17.7"
  lazy val pureConfig = "com.github.pureconfig" %% "pureconfig" % pureConfigVersion
  lazy val pureConfigCatsEffect = "com.github.pureconfig" %% "pureconfig-cats-effect" % pureConfigVersion
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.12"
  lazy val wiremock = "com.github.tomakehurst" % "wiremock-jre8" % "2.33.2"
}
