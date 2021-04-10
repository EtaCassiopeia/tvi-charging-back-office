package com.tvi.charging.model

import com.tvi.charging.model.tariff._
import zio.ZIO
import zio.test.Assertion.equalTo
import zio.test.{DefaultRunnableSpec, ZSpec, assert}

import java.util.Currency

object RefinedDataTypeSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("RefinedDataTypes")(
      testM("should be able to parse a valid energy consumed fee string") {
        for {
          res <- ZIO.fromEither(ConsumedEnergyFreePerKWH.fromString("0.25 EUR / kWh"))
        } yield assert(res)(equalTo(ConsumedEnergyFreePerKWH(Fee.from(0.25).toOption.get, Currency.getInstance("EUR"))))
      },
      testM("should be able to parse numbers without floating point as double") {
        for {
          res <- ZIO.fromEither(ConsumedEnergyFreePerKWH.fromString("1 EUR/kWh"))
        } yield assert(res)(equalTo(ConsumedEnergyFreePerKWH(Fee.from(1).toOption.get, Currency.getInstance("EUR"))))
      },
      testM("should raise an error in case the currency code is invalid") {
        for {
          res <- ZIO.fromEither(ConsumedEnergyFreePerKWH.fromString("0.25 AUR / kWh")).flip
        } yield assert(res)(equalTo("Invalid currency code: `AUR`"))
      },
      testM("should raise the parse failure error for an invalid fee expression") {
        for {
          res <- ZIO.fromEither(ConsumedEnergyFreePerKWH.fromString("0.a5 EUR / kWh")).flip
        } yield assert(res)(
          equalTo(
            "Failed to parse: `0.a5 EUR / kWh`, The fee for the energy consumed should be expressed in currency / kWh format"
          )
        )
      },
      testM("should be able to parse valid parking fee string") {
        for {
          res <- ZIO.fromEither(ParkingFeePerHour.fromString(".3 EUR / hour"))
        } yield assert(res)(equalTo(ParkingFeePerHour(Fee.from(0.3).toOption.get, Currency.getInstance("EUR"))))
      }
    )
}
