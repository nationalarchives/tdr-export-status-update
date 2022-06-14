import Dependencies.{catsEffect, scalaTest, _}

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / name := "tdr-export-status-update"
ThisBuild / organization := "tna"

lazy val root = (project in file("."))
  .settings(
    name := "tdr-export-status-update",
    libraryDependencies ++=
      Seq(
        authUtils,
        awsLambda,
        awsUtils,
        catsEffect,
        circeCore,
        circeParser,
        typeSafeConfig,
        generatedGraphql,
        graphqlClient,
        keycloakCore,
        keycloakAdminClient,
        mockito % Test,
        pureConfig,
        pureConfigCatsEffect,
        scalaTest % Test,
        wiremock % Test
      )
  )

(assembly / assemblyJarName) := "export-status-update.jar"

(assembly / assemblyMergeStrategy) := {
  case PathList("META-INF", xs @_*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

(Test / fork) := true
(Test / javaOptions) += s"-Dconfig.file=${sourceDirectory.value}/test/resources/application.conf"