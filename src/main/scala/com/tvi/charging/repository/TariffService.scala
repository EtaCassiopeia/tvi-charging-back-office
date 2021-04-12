package com.tvi.charging.repository

import com.tvi.charging.model.{AddTariffRequest, BadRequestError, NoActiveTariffNotFoundError, TariffRecord}
import zio.logging.{Logging, log}
import zio.macros.accessible
import zio.{Has, RIO, Ref, ULayer, ZIO}

import java.time.LocalDateTime

@accessible
object TariffService {
  type TariffService = Has[TariffService.Service]
  type Env = Logging

  trait Service {
    def submitTariff(addTariffRequest: AddTariffRequest): RIO[Env, TariffRecord]
    def getActiveTariff(startDateTime: LocalDateTime): RIO[Env, TariffRecord]
  }

  object Service {
    def inMemory(tariffsRef: Ref[List[TariffRecord]]): Service =
      new Service {
        override def submitTariff(addTariffRequest: AddTariffRequest): RIO[Env, TariffRecord] =
          for {
            tariffRecord <- ZIO.succeed(addTariffRequest.toTariffRecord)
            _ <- ZIO.when(addTariffRequest.startsFrom.exists(_.isBefore(LocalDateTime.now()))) {
              ZIO.fail(BadRequestError("Tariff can't be applied retroactively"))
            }
            _ <- log.info(s"Adding tariff $tariffRecord")
            _ <- tariffsRef.update(_ :+ tariffRecord)
          } yield tariffRecord

        override def getActiveTariff(startDateTime: LocalDateTime): RIO[Env, TariffRecord] =
          (for {
            _ <- log.info(s"Looking for a tariff which is active at ${startDateTime.toString}")
            _ <- tariffsRef.get.flatMap(tariffs => log.info(tariffs.mkString(",")))
            tariff <-
              tariffsRef.get.flatMap(list => ZIO.fromOption(list.findLast(_.startsFrom.isBefore(startDateTime))))
          } yield tariff)
            .orElseFail(NoActiveTariffNotFoundError(s"There is no active tariff starting from $startDateTime"))
      }
  }

  val inMemory: ULayer[Has[Service]] = (for {
    tariffsRef <- Ref.make[List[TariffRecord]](List.empty)
    service <- ZIO.succeed(Service.inMemory(tariffsRef))
  } yield service).toLayer
}
