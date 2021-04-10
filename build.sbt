import Dependencies._
import Settings.{commonSettings, compilerSettings, sbtSettings}

lazy val root = (project in file("."))
  .settings(name := "tvi-charging-back-office")
  .settings(commonSettings: _*)
  .settings(compilerSettings: _*)
  .settings(sbtSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Libraries.zio,
      Libraries.zioLogging,
      Libraries.zioLoggingSlf4j,
      Libraries.zioConfig,
      Libraries.refined,
      Libraries.test.zioTest,
      Libraries.test.zioTestSbt,
      Libraries.test.scalaCheck
    ) ++ Libraries.circeModules ++ Libraries.http4sModules
  )
