import Dependencies._
import Settings.{commonSettings, compilerSettings, dockerSettings, sbtSettings}
import sbt.Keys.mainClass

lazy val root = (project in file("."))
  .settings(name := "tvi-charging-back-office")
  .settings(commonSettings: _*)
  .settings(compilerSettings: _*)
  .settings(sbtSettings: _*)
  .settings(dockerSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Libraries.zio,
      Libraries.zioLogging,
      Libraries.zioLoggingSlf4j,
      Libraries.zioCatsInterop,
      Libraries.zioMagic,
      Libraries.zioMacros,
      Libraries.refined,
      Libraries.test.zioTest,
      Libraries.test.zioTestSbt,
      Libraries.test.scalaCheck
    ) ++ Libraries.circeModules ++ Libraries.http4sModules ++ Libraries.zioConfigModules,
    mainClass in Compile := Some("com.tvi.charging.ChargingService")
  )
  .enablePlugins(
    JavaAppPackaging,
    DockerPlugin
  )
