package com.tvi.charging.route

import com.tvi.charging.ChargingService.AppTask
import com.tvi.charging.config.ServerConfig
import com.tvi.charging.model._
import com.tvi.charging.model.tariff.{Fee, ServiceFee}
import com.tvi.charging.repository.{ChargeSessionService, TariffService}
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.server.Router
import org.http4s.{Method, Request}
import zio.clock.Clock
import zio.console._
import zio.duration._
import zio.interop.catz._
import zio.logging.Logging
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test.{DefaultRunnableSpec, assert}
import zio.{ZIO, ZLayer}

import java.time.LocalDateTime
import java.util.Currency

object DriverRouteSpec extends DefaultRunnableSpec {

  val testLayer =
    Logging.console() ++ TariffService.inMemory ++ Clock.live ++ Console.live ++ ChargeSessionService.inMemory ++ ZLayer
      .succeed(ServerConfig("", 0))

  override def spec =
    suite("driver routes suite")(
      testM("should be able to retrieve driver's charging sessions") {
        val driverSessionsService = Router[AppTask]("" -> DriverRoutes.routes)

        val addTariffRequest = AddTariffRequest(
          ConsumedEnergyFeePerKWH(Fee.unsafeFrom(1), Currency.getInstance("EUR")),
          ParkingFeePerHour(Fee.unsafeFrom(1), Currency.getInstance("EUR")),
          ServiceFee.unsafeFrom(0.4)
        )

        val testCase =
          for {
            _ <- TariffService.submitTariff(addTariffRequest)
            startTime = LocalDateTime.now()
            addChargeSession = AddChargeSessionRequest(
              "driver-1",
              startTime.plusSeconds(1),
              startTime.plusSeconds(2),
              ConsumedEnergyPerKWH(100)
            )
            _ <- ZIO.sleep(3.seconds)
            _ <- ChargeSessionService.addChargeSession(addChargeSession)
            rq = Request[AppTask](
              Method.GET,
              uri"/session/driver-1"
            )

            response <-
              driverSessionsService
                .run(rq)
                .value
                .flatMap(_.get.body.compile.toVector.map(x => x.map(_.toChar).mkString("")))
          } yield assert(response)(
            containsString(
              "2021-04-12T12:16:22.036,2021-04-12T12:16:23.036,100.0 kWh,1 EUR/kWh,1 EUR/hour,0.4,100.0 EUR,0.40000 EUR"
            )
          )

        testCase.provideSomeLayer(testLayer)
      }
    ) @@ sequential
}
