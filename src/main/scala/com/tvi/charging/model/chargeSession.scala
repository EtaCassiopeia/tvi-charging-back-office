package com.tvi.charging.model

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.implicits._
import com.tvi.charging.csv.{CsvWriter, toCsv}
import com.tvi.charging.model.tariff.{ConsumedEnergyFreePerKWH, ParkingFeePerHour, ServiceFee, TariffRecord}
import io.circe.{Decoder, Encoder}

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Currency
import scala.util.Try

object chargeSession {

  case class Cost(amount: BigDecimal, currency: Currency) {
    override def toString: String = s"$amount ${currency.getCurrencyCode}"
  }
  case class TotalCost(consumedEnergyCost: Cost, parkingCost: Cost, serviceCost: Cost)

  case class ConsumedEnergyPerKWH(amount: Double) {
    override def toString: String = s"$amount kWh"
  }

  object ConsumedEnergyPerKWH {
    private val consumedEnergyPerKWHRegEx = """(\d+?\.?\d+)\s?kWh""".r

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
    def toChargeSessionRecord(tariff: TariffRecord): ChargeSessionRecord =
      ChargeSessionRecord(driverId, sessionStartTime, sessionEndTime, consumedEnergy, tariff)

    def validate(): Validated[String, AddChargeSessionRequest] = {
      val currentTime = LocalDateTime.now()
      if (sessionStartTime.isAfter(currentTime) && sessionEndTime.isAfter(currentTime))
        Invalid("A charge session can't start nor end in the future")
      else if (sessionStartTime.isAfter(sessionEndTime))
        Invalid("A charge session start time can't be after the end time")
      else
        Valid(this)
    }
  }

  case class ChargeSessionRecord(
    driverId: String,
    sessionStartTime: LocalDateTime,
    sessionEndTime: LocalDateTime,
    consumedEnergy: ConsumedEnergyPerKWH,
    appliedTariff: TariffRecord
  ) {
    def cost: TotalCost = {
      val consumedEnergyCost = Cost(appliedTariff.fee.amount.value * consumedEnergy.amount, appliedTariff.fee.currency)
      val parkingCost = Cost(
        appliedTariff.parkingFee.amount.value * sessionStartTime.until(sessionEndTime, ChronoUnit.HOURS),
        appliedTariff.parkingFee.currency
      )

      val serviceCost = Cost(
        ((consumedEnergyCost.amount + parkingCost.amount) * appliedTariff.serviceFee.value / 100)
          .setScale(5, BigDecimal.RoundingMode.HALF_UP),
        appliedTariff.fee.currency
      )

      TotalCost(consumedEnergyCost, parkingCost, serviceCost)
    }
  }

  case class ChargeSessionResponse(
    sessionStartTime: LocalDateTime,
    sessionEndTime: LocalDateTime,
    consumedEnergy: ConsumedEnergyPerKWH,
    appliedConsumedEnergyFreePerKWH: ConsumedEnergyFreePerKWH,
    appliedParkingFeePerHour: ParkingFeePerHour,
    appliedServiceFee: ServiceFee,
    totalPrice: Cost,
    totalServiceCost: Cost
  )

  case class ChargeSessionsResponse(session: List[ChargeSessionResponse])

  implicit val chargeSessionResponseCsvWriter: CsvWriter[ChargeSessionsResponse] =
    (response: ChargeSessionsResponse) => toCsv(response.session)

  implicit val consumedEnergyEncoder: Encoder[ConsumedEnergyPerKWH] = Encoder[String].contramap(_.toString)
  implicit val consumedEnergyDecoder: Decoder[ConsumedEnergyPerKWH] =
    Decoder[String].emap(ConsumedEnergyPerKWH.fromString)
}
