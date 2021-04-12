package com.tvi.charging.repository

import com.tvi.charging.model.tariff.{Fee, ServiceFee}
import com.tvi.charging.model.{AddTariffRequest, BadRequestError, ConsumedEnergyFeePerKWH, ParkingFeePerHour}
import zio.ZIO
import zio.logging.Logging
import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, ZSpec, assert}

import java.time.LocalDateTime
import java.util.Currency

object TariffServiceSpec extends DefaultRunnableSpec {

  val testLayer = Logging.console() ++ TariffService.inMemory

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("TariffService")(
      testM("should be able to successfully store and retrieve a Tariff") {
        for {
          expected <- ZIO.succeed(
            AddTariffRequest(
              ConsumedEnergyFeePerKWH(Fee.unsafeFrom(1), Currency.getInstance("EUR")),
              ParkingFeePerHour(Fee.unsafeFrom(1), Currency.getInstance("EUR")),
              ServiceFee.unsafeFrom(0.4)
            )
          )
          _ <- TariffService.submitTariff(expected)
          currentDate = LocalDateTime.now()
          retrievedTariffFeeAmount <- TariffService.getActiveTariff(currentDate).map(_.fee)
        } yield assert(retrievedTariffFeeAmount)(equalTo(expected.fee))
      },
      testM("should raise an error in case some of the input parameters are not valid") {
        for {
          expectedError <-
            ZIO
              .succeed(
                AddTariffRequest(
                  ConsumedEnergyFeePerKWH(Fee.unsafeFrom(1), Currency.getInstance("EUR")),
                  ParkingFeePerHour(Fee.unsafeFrom(1), Currency.getInstance("EUR")),
                  ServiceFee.unsafeFrom(1)
                )
              )
              .run
        } yield assert(expectedError)(dies(isSubtype[Exception](anything)))
      },
      testM("should be able reject the request if the start time of the tariff is before now") {
        for {
          tariff <- ZIO.succeed(
            AddTariffRequest(
              ConsumedEnergyFeePerKWH(Fee.unsafeFrom(1), Currency.getInstance("EUR")),
              ParkingFeePerHour(Fee.unsafeFrom(1), Currency.getInstance("EUR")),
              ServiceFee.unsafeFrom(0.4),
              Some(LocalDateTime.now().minusDays(1))
            )
          )
          expectedError <- TariffService.submitTariff(tariff).run
        } yield assert(expectedError)(fails(isSubtype[BadRequestError](anything)))
      }
    ).provideSomeLayerShared(testLayer)
}
