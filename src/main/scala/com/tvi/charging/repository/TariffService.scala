package com.tvi.charging.repository

import com.tvi.charging.model.tariff.Tariff
import com.tvi.charging.model.{BadRequestError, NoActiveTariffNotFoundError}
import zio.logging.{Logging, log}
import zio.macros.accessible
import zio.{Has, RIO, Ref, UIO, ULayer, ZIO, ZLayer}

import java.time.LocalDateTime

@accessible
object TariffService {
  type TariffService = Has[TariffService.Service]
  type Env = Logging

  trait Service {
    def submitTariff(tariff: Tariff): RIO[Env, Unit]
    def getActiveTariff(startDateTime: LocalDateTime): RIO[Env, Tariff]
  }

  object Service {
    val inMemory: Service =
      new Service {
        val tariffsRef: UIO[Ref[List[Tariff]]] = Ref.make[List[Tariff]](List.empty)
        override def submitTariff(tariff: Tariff): RIO[Env, Unit] =
          for {
            _ <- ZIO.when(tariff.startsFrom.isBefore(LocalDateTime.now())) {
              ZIO.fail(BadRequestError("Tariff can't be applied retroactively"))
            }
            _ <- log.info(s"Adding tariff $tariff")
            tariffs <- tariffsRef
            _ <- tariffs.update(_ :+ tariff)
          } yield ()

        override def getActiveTariff(startDateTime: LocalDateTime): RIO[Env, Tariff] =
          (for {
            _ <- log.info(s"Looking for an active tariff at ${startDateTime.toString}")
            tariffs <- tariffsRef
            tariff <- tariffs.get.flatMap(list => ZIO.fromOption(list.findLast(_.startsFrom.isBefore(startDateTime))))
          } yield tariff)
            .orElseFail(NoActiveTariffNotFoundError(s"There is no active tariff starts from $startDateTime"))
      }
  }

  val inMemory: ULayer[Has[Service]] = ZLayer.succeed(Service.inMemory)
}
