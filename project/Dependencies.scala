import sbt._

object Dependencies {

  object Versions {
    val zioVersion = "1.0.5"
    val zioLoggingVersion = "0.5.8"
    val zioCatsInteropVersion = "2.4.0.0"
    val https4Version = "1.0-234-d1a2b53"
    val circeVersion = "0.13.0"
    val log4jVersion = "2.13.1"
    val scalaTestVersion = "3.2.2"
    val refinedVersion = "0.9.20"
    val zioConfigVersion = "1.0.4"
    val zioMagicVersion = "0.2.3"
    val zioMacrosVersion = "0.6.2"
  }

  object Libraries {

    import Versions._

    val zio = "dev.zio" %% "zio" % zioVersion
    val zioCatsInterop = "dev.zio" %% "zio-interop-cats" % zioCatsInteropVersion
    val zioLogging = "dev.zio" %% "zio-logging" % zioLoggingVersion
    val zioLoggingSlf4j = "dev.zio" %% "zio-logging-slf4j" % zioLoggingVersion

    val zioMacros = "dev.zio" %% "zio-macros" % zioVersion

    private val zioConfig: String => ModuleID = artifact => "dev.zio" %% artifact % zioConfigVersion
    val zioConfigModules =
      Seq("zio-config", "zio-config-magnolia", "zio-config-typesafe").map(zioConfig)

    val zioMagic = "io.github.kitlangton" %% "zio-magic" % zioMagicVersion

    private val http4s: String => ModuleID = artifact => "org.http4s" %% artifact % https4Version
    val http4sModules = Seq("http4s-blaze-server", "http4s-dsl", "http4s-circe").map(http4s)

    private val log4j: String => ModuleID = artifact => "org.apache.logging.log4j" % artifact % log4jVersion

    val log4jModules: Seq[sbt.ModuleID] = Seq("log4j-api", "log4j-core", "log4j-slf4j-impl").map(log4j)

    private val circeModule: String => ModuleID = artifact => "io.circe" %% artifact % circeVersion

    val circeModules: Seq[sbt.ModuleID] =
      Seq("circe-core", "circe-parser", "circe-generic", "circe-generic-extras", "circe-shapes", "circe-refined").map(
        circeModule
      )

    val refined = "eu.timepit" %% "refined" % refinedVersion

    object test {
      val zioTest = "dev.zio" %% "zio-test" % zioVersion % Test
      val zioTestSbt = "dev.zio" %% "zio-test-sbt" % zioVersion % Test
      val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.14.2" % Test
    }
  }
}
