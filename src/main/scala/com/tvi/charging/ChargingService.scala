package com.tvi.charging

import com.tvi.charging.Http4Server.Http4Server
import com.tvi.charging.config.ServerConfig
import com.tvi.charging.repository.TariffService
import com.tvi.charging.repository.TariffService.TariffService
import org.http4s.server.Server
import zio._
import zio.clock.Clock
import zio.console._
import zio.logging.Logging
import zio.magic.ZioProvideMagicOps

object ChargingService extends App {

  type AppEnvironment = TariffService with Has[ServerConfig] with Clock with Logging
  type AppTask[A] = RIO[AppEnvironment, A]

  def run(args: List[String]): URIO[ZEnv, ExitCode] = {

    val program: ZIO[Has[Server] with AppEnvironment, Nothing, Nothing] =
      ZIO.never

    val httpServerLayer: ZLayer[AppEnvironment, Throwable, Http4Server] =
      Http4Server.createHttp4sLayer()

    program
      .inject(
        Logging.console(),
        Clock.live,
        Console.live,
        TariffService.inMemory,
        ServerConfig.configLayer,
        httpServerLayer
      )
      .foldCauseM(cause => putStrLn(cause.prettyPrint), _ => ZIO.unit)
      .exitCode
  }
}
