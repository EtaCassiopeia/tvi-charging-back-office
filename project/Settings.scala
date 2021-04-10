import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtOnCompile
import sbt.Keys.{parallelExecution, _}
import sbt._
import sbt.util.Level

import scala.language.postfixOps

object Settings {

  lazy val compilerSettings =
    Seq(
      scalaVersion := "2.13.5",
      javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
      scalacOptions := Seq(
        "-Ymacro-annotations",
        "-deprecation",
        "-encoding",
        "utf-8",
        "-explaintypes",
        "-feature",
        "-unchecked",
        "-language:postfixOps",
        "-language:higherKinds",
        "-language:implicitConversions",
        "-Xcheckinit",
        "-Xfatal-warnings"
      ),
      logLevel := Level.Info,
      version := "0.1.0",
      scalafmtOnCompile := true,
      testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
    )

  lazy val sbtSettings =
    Seq(
      fork := true,
      parallelExecution in Test := false,
      cancelable in Global := true
    )

  lazy val commonSettings =
    compilerSettings ++
      sbtSettings ++ Seq(
      organization := "com.tvi",
      resolvers ++= Seq(
        Resolver.mavenLocal,
        "Confluent".at("https://packages.confluent.io/maven/"),
        "jitpack".at("https://jitpack.io"),
        Resolver.jcenterRepo
      )
    )
}
