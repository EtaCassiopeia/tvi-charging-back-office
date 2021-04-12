package com.tvi.charging.route

import com.tvi.charging.ChargingService.AppTask
import com.tvi.charging.config.ServerConfig
import com.tvi.charging.model.tariff.{Fee, ServiceFee}
import com.tvi.charging.model._
import com.tvi.charging.repository.{ChargeSessionService, TariffService}
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.headers.Authorization
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.server.Router
import org.http4s.{Method, Request, _}
import zio.clock.Clock
import zio.console._
import zio.duration._
import zio.interop.catz._
import zio.logging.Logging
import zio.test.Assertion._
import zio.test.TestAspect.sequential
import zio.test.{DefaultRunnableSpec, assert}
import zio.{ZIO, ZLayer}
import com.tvi.charging.model.implicits._
import io.circe.refined._

import java.time.LocalDateTime
import java.util.Currency

object RoutesSpec extends DefaultRunnableSpec {

  val testLayer =
    Logging.console() ++ TariffService.inMemory ++ Clock.live ++ Console.live ++ ChargeSessionService.inMemory ++ ZLayer
      .succeed(ServerConfig("", 0))

  override def spec =
    suite("routes suite")(
      testM("should reject the request due to the authentication error") {
        val addTariffRequest = AddTariffRequest(
          ConsumedEnergyFeePerKWH(Fee.unsafeFrom(1), Currency.getInstance("EUR")),
          ParkingFeePerHour(Fee.unsafeFrom(1), Currency.getInstance("EUR")),
          ServiceFee.unsafeFrom(0.4)
        )
        val rq = Request[AppTask](Method.POST, uri"/tariff").withEntity(addTariffRequest)
        val tariffService = Router[AppTask]("" -> Routes.tariffRoutes)

        val testCase = for {
          response <- tariffService.run(rq).map(_.status).value.map(_.get)
        } yield assert(response)(equalTo(Status.Unauthorized))

        testCase.provideSomeLayer(testLayer)
      },
      testM("should be able to register a submitted tariff") {
        val addTariffRequest = AddTariffRequest(
          ConsumedEnergyFeePerKWH(Fee.unsafeFrom(1), Currency.getInstance("EUR")),
          ParkingFeePerHour(Fee.unsafeFrom(1), Currency.getInstance("EUR")),
          ServiceFee.unsafeFrom(0.4)
        )
        val rq = Request[AppTask](
          Method.POST,
          uri"/tariff",
          headers = Headers(List(Authorization(BasicCredentials("username", "password"))))
        ).withEntity(addTariffRequest)
        val tariffService = Router[AppTask]("" -> Routes.tariffRoutes)

        val testCase = for {
          response <- tariffService.run(rq).map(_.status).value.map(_.get)
        } yield assert(response)(equalTo(Status.Created))

        testCase.provideSomeLayer(testLayer)
      },
      testM(
        "should return a NotFound error if there isn't any active tariff at the time the charging session submitted"
      ) {
        val chargeSessionService = Router[AppTask]("" -> Routes.chargeSessionRoutes)

        val startTime = LocalDateTime.now()
        val addChargeSession =
          AddChargeSessionRequest(
            "driver-1",
            startTime.plusSeconds(1),
            startTime.plusSeconds(2),
            ConsumedEnergyPerKWH(100)
          )
        val rq = Request[AppTask](
          Method.POST,
          uri"/session"
        ).withEntity(addChargeSession)

        val testCase = for {
          _ <- ZIO.sleep(3.seconds)
          response <- chargeSessionService.run(rq).map(_.status).value.map(_.get)
        } yield assert(response)(equalTo(Status.NotFound))

        testCase.provideSomeLayer(testLayer)
      },
      testM("should be able to register a submitted charge session") {
        val chargeSessionService = Router[AppTask]("" -> Routes.chargeSessionRoutes)

        val addTariffRequest = AddTariffRequest(
          ConsumedEnergyFeePerKWH(Fee.unsafeFrom(1), Currency.getInstance("EUR")),
          ParkingFeePerHour(Fee.unsafeFrom(1), Currency.getInstance("EUR")),
          ServiceFee.unsafeFrom(0.4)
        )

        val testCase = for {
          _ <- TariffService.submitTariff(addTariffRequest)
          startTime = LocalDateTime.now()
          addChargeSession = AddChargeSessionRequest(
            "driver-1",
            startTime.plusSeconds(1),
            startTime.plusSeconds(2),
            ConsumedEnergyPerKWH(100)
          )
          rq = Request[AppTask](
            Method.POST,
            uri"/session"
          ).withEntity(addChargeSession)
          _ <- ZIO.sleep(3.seconds)
          response <- chargeSessionService.run(rq).map(_.status).value.map(_.get)
        } yield assert(response)(equalTo(Status.Created))

        testCase.provideSomeLayer(testLayer)
      }
    ) @@ sequential
}
