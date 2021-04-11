package com.tvi.charging.route

import cats.Monad
import com.tvi.charging.ChargingService.AppTask
import com.tvi.charging.csv.CsvWriter
import com.tvi.charging.repository.ChargeSessionService
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{`Content-Disposition`, `Content-Type`}
import zio.interop.catz._
import zio.logging.log

object DriverRoutes {

  private val dsl = Http4sDsl[AppTask]
  import dsl._

  implicit def csvEntityEncoder[F[_]: Monad, X](implicit writer: CsvWriter[X]): EntityEncoder[F, X] =
    EntityEncoder
      .stringEncoder[F]
      .contramap(writer.write)
      .withContentType(`Content-Type`(MediaType.text.csv))

  private[route] val routes =
    HttpRoutes
      .of[AppTask] {
        case GET -> Root / "session" / driverId =>
          for {
            _ <- log.info("Reporting charge sessions")
            resp <-
              ChargeSessionService
                .getChargeSessionsByDriverId(driverId)
                .flatMap(sessions =>
                  Ok(sessions).map(
                    _.putHeaders(
                      Header("Content-Type", "text/csv"),
                      `Content-Disposition`("attachment", Map("filename" -> "charge-sessions-report.csv"))
                    )
                  )
                )
          } yield resp
      }
}
