package com.tvi.charging.repository

import com.tvi.charging.model.BadRequestError
import com.tvi.charging.model.chargeSession.{AddChargeSessionRequest, ChargeSessionRecord}
import com.tvi.charging.repository.TariffService.TariffService
import zio.logging.{Logging, log}
import zio.macros.accessible
import zio.{Has, RIO, Ref, ULayer, ZIO, ZLayer}

import java.time.LocalDateTime

@accessible
object ChargeSessionService {
  type ChargeSessionService = Has[ChargeSessionService.Service]
  type Env = TariffService with Logging

  trait Service {
    def addChargeSession(addChargeSessionRequest: AddChargeSessionRequest): RIO[Env, Unit]
    def getChargeSessions(
      startDateTime: Option[LocalDateTime] = None,
      endDateTime: Option[LocalDateTime] = None
    ): RIO[Env, List[ChargeSessionRecord]]
    def getChargeSessionsByDriverId(
      driverId: String,
      startDateTime: Option[LocalDateTime] = None,
      endDateTime: Option[LocalDateTime] = None
    ): RIO[Env, List[ChargeSessionRecord]]
  }

  object Service {
    val inMemory: Service =
      new Service {
        private val chargeSessions = Ref.make(List.empty[ChargeSessionRecord])

        override def addChargeSession(addChargeSessionRequest: AddChargeSessionRequest): RIO[Env, Unit] =
          for {
            currentTime <- ZIO.succeed(LocalDateTime.now())
            _ <- ZIO.when(!addChargeSessionRequest.validate()) {
              ZIO.fail(BadRequestError("A charge session can't start nor end in the future"))
            }
            activeTariff <- TariffService.getActiveTariff(addChargeSessionRequest.sessionStartTime)
            _ <- log.info(s"Adding charge session $addChargeSessionRequest")
            sessions <- chargeSessions
            _ <- sessions.update(_ :+ addChargeSessionRequest.toChargeSessionRecord(activeTariff))
          } yield ()

        override def getChargeSessions(
          startDateTime: Option[LocalDateTime],
          endDateTime: Option[LocalDateTime]
        ): RIO[Env, List[ChargeSessionRecord]] = ???

        override def getChargeSessionsByDriverId(
          driverId: String,
          startDateTime: Option[LocalDateTime],
          endDateTime: Option[LocalDateTime]
        ): RIO[Env, List[ChargeSessionRecord]] = ???
      }
  }

  val inMemory: ULayer[Has[Service]] = ZLayer.succeed(Service.inMemory)
}
