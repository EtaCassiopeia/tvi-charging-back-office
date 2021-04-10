package com.tvi.charging.repository

import com.tvi.charging.model.tariff.Tariff
import zio.logging.{Logging, log}
import zio.macros.accessible
import zio.{Has, ULayer, URIO, ZLayer}

@accessible
object TariffService {
  type TariffService = Has[TariffService.Service]
  type Env = Logging

  trait Service {
    def submitTariff(tariff: Tariff): URIO[Env, Unit]
  }

  object Service {
    val inMemory =
      new Service {
        override def submitTariff(tariff: Tariff): URIO[Env, Unit] = log.info(s"Adding tariff $tariff")
      }
  }

  val inMemory: ULayer[Has[Service]] = ZLayer.succeed(Service.inMemory)
}
