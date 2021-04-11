package com.tvi.charging.repository

import cats.data.Validated.Invalid
import com.tvi.charging.model._
import com.tvi.charging.repository.TariffService.TariffService
import zio.logging.{Logging, log}
import zio.macros.accessible
import zio.{Has, RIO, Ref, ULayer, ZIO}

@accessible
object ChargeSessionService {
  type ChargeSessionService = Has[ChargeSessionService.Service]
  type Env = TariffService with Logging

  trait Service {
    def addChargeSession(addChargeSessionRequest: AddChargeSessionRequest): RIO[Env, Unit]
    def getChargeSessionsByDriverId(driverId: String): RIO[Env, ChargeSessionsResponse]
  }

  object Service {
    def inMemory(chargeSessions: Ref[List[ChargeSessionRecord]]): Service =
      new Service {
        override def addChargeSession(addChargeSessionRequest: AddChargeSessionRequest): RIO[Env, Unit] =
          for {
            _ <- ZIO.whenCase(addChargeSessionRequest.validate()) {
              case Invalid(error) =>
                ZIO.fail(BadRequestError(error))
              case _ => ZIO.unit
            }
            activeTariff <- TariffService.getActiveTariff(addChargeSessionRequest.sessionStartTime)
            _ <- log.info(s"Adding charge session $addChargeSessionRequest")
            _ <- chargeSessions.update(_ :+ addChargeSessionRequest.toChargeSessionRecord(activeTariff))
          } yield ()

        override def getChargeSessionsByDriverId(driverId: String): RIO[Env, ChargeSessionsResponse] =
          for {
            sessions <- chargeSessions.get.map(_.filter(_.driverId == driverId))
            response = sessions.map { record =>
              import record._
              val totalCost = record.cost
              ChargeSessionResponse(
                sessionStartTime,
                sessionEndTime,
                consumedEnergy,
                appliedTariff.fee,
                appliedTariff.parkingFee,
                appliedTariff.serviceFee,
                Cost(
                  totalCost.consumedEnergyCost.amount + totalCost.parkingCost.amount,
                  totalCost.consumedEnergyCost.currency
                ),
                totalCost.serviceCost
              )
            }
          } yield ChargeSessionsResponse(response)
      }
  }

  val inMemory: ULayer[Has[Service]] = (for {
    chargeSessions <- Ref.make(List.empty[ChargeSessionRecord])
    service <- ZIO.succeed(Service.inMemory(chargeSessions))
  } yield service).toLayer
}
