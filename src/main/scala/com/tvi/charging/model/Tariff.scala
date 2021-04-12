package com.tvi.charging.model

import cats.implicits._
import com.tvi.charging.model.CurrencyHelper.getCurrencyInstance
import com.tvi.charging.model.tariff.{Fee, ServiceFee, parseFeeExpression}
import eu.timepit.refined.api.{Refined, RefinedTypeOps}
import eu.timepit.refined.numeric.{Interval, Positive}
import io.circe.{Decoder, Encoder}

import java.time.LocalDateTime
import java.util.Currency
import scala.util.Try
import scala.util.matching.Regex

case class ConsumedEnergyFeePerKWH(amount: Fee, currency: Currency) {
  override def toString: String = s"$amount ${currency.getCurrencyCode}/kWh"
}

object ConsumedEnergyFeePerKWH {
  private val consumedEnergyFreePerKWHRegEx = """(\d*?\.?\d*)\s?([A-Z]{3})\s?/\s?kWh""".r

  def fromString(value: String): Either[String, ConsumedEnergyFeePerKWH] =
    parseFeeExpression(
      value,
      consumedEnergyFreePerKWHRegEx,
      value =>
        s"Failed to parse: `$value`, The fee for the energy consumed should be expressed in currency / kWh format",
      ConsumedEnergyFeePerKWH.apply
    )

  implicit val consumedEnergyFeePerKWHEncoder: Encoder[ConsumedEnergyFeePerKWH] = Encoder[String].contramap(_.toString)
  implicit val consumedEnergyFeePerKWHDecoder: Decoder[ConsumedEnergyFeePerKWH] =
    Decoder[String].emap(ConsumedEnergyFeePerKWH.fromString)
}

case class ParkingFeePerHour(amount: Fee, currency: Currency) {
  override def toString: String = s"$amount ${currency.getCurrencyCode}/hour"
}

object ParkingFeePerHour {
  private val parkingFeePerHourRegEx = """(\d*?\.?\d*)\s?([A-Z]{3})\s?/\s?hour""".r

  def fromString(value: String): Either[String, ParkingFeePerHour] =
    parseFeeExpression(
      value,
      parkingFeePerHourRegEx,
      value => s"Failed to parse: `$value`, A parking fee should be expressed in currency / hour format",
      ParkingFeePerHour.apply
    )

  implicit val parkingFeePerHourEncoder: Encoder[ParkingFeePerHour] = Encoder[String].contramap(_.toString)
  implicit val parkingFeePerHourDecoder: Decoder[ParkingFeePerHour] = Decoder[String].emap(ParkingFeePerHour.fromString)
}

case class AddTariffRequest(
  fee: ConsumedEnergyFeePerKWH,
  parkingFee: ParkingFeePerHour,
  serviceFee: ServiceFee,
  startsFrom: Option[LocalDateTime] = None
) {
  def toTariffRecord: TariffRecord =
    TariffRecord(fee, parkingFee, serviceFee, startsFrom.getOrElse(LocalDateTime.now()))
}

case class TariffRecord(
  fee: ConsumedEnergyFeePerKWH,
  parkingFee: ParkingFeePerHour,
  serviceFee: ServiceFee,
  startsFrom: LocalDateTime
)

case class HttpCreatedResponse(details: String)

object tariff {

  type Fee = BigDecimal Refined Positive
  object Fee extends RefinedTypeOps[Fee, BigDecimal]

  type ServiceFee = Double Refined Interval.OpenClosed[0.0d, 0.5d]
  object ServiceFee extends RefinedTypeOps[ServiceFee, Double]

  private[model] def parseFeeExpression[A](
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
