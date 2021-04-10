package com.tvi.charging

import com.tvi.charging.ChargingService.{AppEnvironment, AppTask}
import com.tvi.charging.config.ServerConfig
import com.tvi.charging.route.Routes
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder
import zio._
import zio.config.getConfig
import zio.interop.catz._

object Http4Server {

  type Http4Server = Has[Server]

  def createHttp4Server(): ZManaged[AppEnvironment, Throwable, Server] =
    ZManaged.runtime[AppEnvironment].flatMap { implicit runtime: Runtime[AppEnvironment] =>
      for {
        serverConfig <- getConfig[ServerConfig].toManaged_
        server <-
          BlazeServerBuilder[AppTask](runtime.platform.executor.asEC)
            .bindHttp(serverConfig.port, serverConfig.host)
            .withHttpApp(Routes.tariffService())
            .resource
            .toManagedZIO
      } yield server
    }

  def createHttp4sLayer(): ZLayer[AppEnvironment, Throwable, Http4Server] = ZLayer.fromManaged(createHttp4Server())
}
