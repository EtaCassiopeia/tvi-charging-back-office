package com.tvi.charging.model

import com.tvi.charging.model.chargeSession.{AddChargeSessionRequest, ConsumedEnergyPerKWH}
import io.circe.generic.auto._
import io.circe.parser.decode
import zio.ZIO
import zio.test.Assertion.equalTo
import zio.test.{DefaultRunnableSpec, ZSpec, assert}
import com.tvi.charging.model.implicits._
import com.tvi.charging.model.chargeSession._
import com.tvi.charging.model.tariff.{ConsumedEnergyFreePerKWH, Fee, ParkingFeePerHour, ServiceFee, TariffRecord}

import java.time.LocalDateTime
import java.util.Currency

object ChargeSessionSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("ChargeSession")(
      testM("should be able to parse a valid AddChargeSessionRequest") {
        import testContext._
        for {
          res <- ZIO.fromEither(
            decode[AddChargeSessionRequest](
              """{"driverId":"driver-1","sessionStartTime":"2021-04-10T22:20:50Z","sessionEndTime":"2021-04-10T23:20:50Z","consumedEnergy":"100.0 kWh"}"""
            )
          )
        } yield assert(res)(
          equalTo(AddChargeSessionRequest("driver-1", defaultStartTime, defaultEndTime, ConsumedEnergyPerKWH(100)))
        )
      },
      testM("should be able calculate the total charging cost from ChargeSessionRecord") {
        import testContext._
        for {
          chargeSessionRequest <- ZIO.succeed(
            AddChargeSessionRequest(
              "driver-1",
              defaultStartTime,
              defaultEndTime,
              ConsumedEnergyPerKWH(100)
            )
          )
          tariff <- ZIO.succeed(
            TariffRecord(
              ConsumedEnergyFreePerKWH(Fee.unsafeFrom(0.25), Currency.getInstance("EUR")),
              ParkingFeePerHour(Fee.unsafeFrom(0.01), Currency.getInstance("EUR")),
              ServiceFee.unsafeFrom(0.4),
              defaultStartTime
            )
          )
          chargeSessionRecord = chargeSessionRequest.toChargeSessionRecord(tariff)
          totalCost = chargeSessionRecord.cost
        } yield assert(totalCost.consumedEnergyCost.amount)(equalTo(BigDecimal(25))) && assert(
          totalCost.parkingCost.amount
        )(equalTo(BigDecimal(0.01))) && assert(totalCost.serviceCost.amount)(equalTo(BigDecimal(0.10004)))
      }
    )

  private val testContext = new {
    val defaultStartTime: LocalDateTime = LocalDateTime.of(2021, 4, 10, 22, 20, 50)
    val defaultEndTime: LocalDateTime = defaultStartTime.plusHours(1)
  }
}
