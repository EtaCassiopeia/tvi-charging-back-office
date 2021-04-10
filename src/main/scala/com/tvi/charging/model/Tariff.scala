package com.tvi.charging.model

import cats.implicits._
import com.tvi.charging.model.CurrencyHelper.getCurrencyInstance
import eu.timepit.refined.api.{Refined, RefinedTypeOps}
import eu.timepit.refined.numeric.{Interval, Positive}
import io.circe.{Decoder, Encoder, Json}

import java.util.Currency
import scala.util.Try
import scala.util.matching.Regex

object tariff {

  type Fee = Double Refined Positive
  object Fee extends RefinedTypeOps[Fee, Double]

  type ServiceFee = Double Refined Interval.OpenClosed[0.0d, 0.5d]
  object ServiceFee extends RefinedTypeOps[ServiceFee, Double]

  case class ConsumedEnergyFreePerKWH(amount: Fee, currency: Currency) {
    override def toString: String = s"$amount ${currency.getSymbol}/kWh"
  }

  object ConsumedEnergyFreePerKWH {
    private val consumedEnergyFreePerKWHRegEx = """(\d?\.?\d+)\s?([A-Z]{3})\s?/\s?kWh""".r

    def fromString(value: String): Either[String, ConsumedEnergyFreePerKWH] =
      parseFeeExpression(
        value,
        consumedEnergyFreePerKWHRegEx,
        value =>
          s"Failed to parse: `$value`, The fee for the energy consumed should be expressed in currency / kWh format",
        ConsumedEnergyFreePerKWH.apply
      )
  }

  case class ParkingFeePerHour(amount: Fee, currency: Currency) {
    override def toString: String = s"$amount ${currency.getSymbol}/hour"
  }

  object ParkingFeePerHour {
    private val parkingFeePerHourRegEx = """(\d?\.?\d+)\s?([A-Z]{3})\s?/\s?hour""".r

    def fromString(value: String): Either[String, ParkingFeePerHour] =
      parseFeeExpression(
        value,
        parkingFeePerHourRegEx,
        value => s"Failed to parse: `$value`, A parking fee should be expressed in currency / hour format",
        ParkingFeePerHour.apply
      )
  }

  case class Tariff(fee: ConsumedEnergyFreePerKWH, parkingFee: ParkingFeePerHour, serviceFee: ServiceFee)

  implicit val consumedEnergyFeePerKWHEncoder: Encoder[ConsumedEnergyFreePerKWH] = c => Json.fromString(c.toString)
  implicit val consumedEnergyFeePerKWHDecoder: Decoder[ConsumedEnergyFreePerKWH] =
    Decoder.decodeString.emap(ConsumedEnergyFreePerKWH.fromString)

  implicit val parkingFeePerHourEncoder: Encoder[ParkingFeePerHour] = c => Json.fromString(c.toString)
  implicit val parkingFeePerHourDecoder: Decoder[ParkingFeePerHour] =
    Decoder.decodeString.emap(ParkingFeePerHour.fromString)

  private def parseFeeExpression[A](
    FeeExpression: String,
    regEx: Regex,
    errorMessage: String => String,
    apply: (Fee, Currency) => A
  ): Either[String, A] =
    Try {
      val regEx(amount, currencyCode) = FeeExpression
      (amount.toDouble, currencyCode)
    }.toEither
      .leftMap(_ => errorMessage(FeeExpression))
      .flatMap {
        case (fee, currencyCode) =>
          (Fee.from(fee), getCurrencyInstance(currencyCode)).mapN(apply)
      }
}
