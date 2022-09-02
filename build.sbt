import Dependencies._

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / name := "tdr-export-status-update"
ThisBuild / organization := "uk.gov.nationalarchives"

lazy val root = (project in file("."))
  .settings(
    name := "tdr-export-status-update",
    libraryDependencies ++=
      Seq(
        authUtils,
        awsLambda,
        awsSsm,
        awsUtils,
        circeCore,
        circeParser,
        typeSafeConfig,
        generatedGraphql,
        graphqlClient,
        mockito % Test,
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
