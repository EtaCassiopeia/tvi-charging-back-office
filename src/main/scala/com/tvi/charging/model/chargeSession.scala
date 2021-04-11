package com.tvi.charging.model

import cats.implicits._
import com.tvi.charging.model.tariff.Tariff
import io.circe.{Decoder, Encoder}

import java.time.LocalDateTime
import scala.util.Try

object chargeSession {

  case class ConsumedEnergyPerKWH(amount: Double) {
    override def toString: String = s"$amount kWh"
  }

  object ConsumedEnergyPerKWH {
    private val consumedEnergyPerKWHRegEx = """(\d?\.?\d+)\s?kWh""".r

    def fromString(expression: String): Either[String, ConsumedEnergyPerKWH] =
      Try {
        val consumedEnergyPerKWHRegEx(amount) = expression
        ConsumedEnergyPerKWH(amount.toDouble)
      }.toEither
        .leftMap(_ => s"Failed to parse: `$expression`, The consumed energy should be expressed in kWh format")
  }

  case class AddChargeSessionRequest(
    driverId: String,
    sessionStartTime: LocalDateTime,
    sessionEndTime: LocalDateTime,
    consumedEnergy: ConsumedEnergyPerKWH
  ) {
    def toChargeSessionRecord(tariff: Tariff): ChargeSessionRecord =
      ChargeSessionRecord(driverId, sessionStartTime, sessionEndTime, consumedEnergy, tariff)

    def validate(): Boolean = {
      val currentTime = LocalDateTime.now()
      sessionStartTime.isBefore(currentTime) || sessionEndTime.isBefore(currentTime)
    }
  }

  case class ChargeSessionRecord(
    driverId: String,
    sessionStartTime: LocalDateTime,
    sessionEndTime: LocalDateTime,
    consumedEnergy: ConsumedEnergyPerKWH,
    appliedTariff: Tariff
  )

  implicit val consumedEnergyEncoder: Encoder[ConsumedEnergyPerKWH] = Encoder[String].contramap(_.toString)
  implicit val consumedEnergyDecoder: Decoder[ConsumedEnergyPerKWH] =
    Decoder[String].emap(ConsumedEnergyPerKWH.fromString)
}
