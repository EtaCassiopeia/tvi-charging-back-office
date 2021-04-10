package com.tvi.charging.route

import cats.data.Kleisli
import com.tvi.charging.ChargingService.AppTask
import com.tvi.charging.model.tariff._
import com.tvi.charging.repository.TariffService
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.refined._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import zio.interop.catz._
import zio.logging.log

object Routes {

  private val dsl = Http4sDsl[AppTask]
  import dsl._

  private implicit def encoder[A](implicit D: Encoder[A]): EntityEncoder[AppTask, A] = jsonEncoderOf[AppTask, A]
  private implicit def decoder[A](implicit D: Decoder[A]): EntityDecoder[AppTask, A] = jsonOf[AppTask, A]

  def tariffService(): Kleisli[AppTask, Request[AppTask], Response[AppTask]] =
    HttpRoutes
      .of[AppTask] {
        case req @ POST -> Root / "tariff" =>
          for {
            tariff <- req.as[Tariff].tapError(t => log.error(t.getMessage))
            _ <- log.info(s"Handling request $tariff")
            _ <- TariffService.submitTariff(tariff)
            resp <- Ok(tariff.toString)
          } yield resp
      }
      .orNotFound
}
