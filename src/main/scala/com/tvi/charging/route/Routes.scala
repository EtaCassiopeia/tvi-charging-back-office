package com.tvi.charging.route

import cats.data.Kleisli
import cats.implicits.toSemigroupKOps
import com.tvi.charging.ChargingService.AppTask
import com.tvi.charging.model.{BadRequestError, NoActiveTariffNotFoundError}
import com.tvi.charging.model.chargeSession.AddChargeSessionRequest
import com.tvi.charging.model.tariff._
import com.tvi.charging.model.implicits._
import com.tvi.charging.repository.{ChargeSessionService, TariffService}
import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import io.circe.refined._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.AuthMiddleware
import org.http4s.server.middleware.authentication.BasicAuth
import org.http4s.server.middleware.authentication.BasicAuth.BasicAuthenticator
import zio.RIO
import zio.interop.catz._
import zio.logging.log

object Routes {

  private val dsl = Http4sDsl[AppTask]
  import dsl._

  private implicit def encoder[A](implicit D: Encoder[A]): EntityEncoder[AppTask, A] = jsonEncoderOf[AppTask, A]
  private implicit def decoder[A](implicit D: Decoder[A]): EntityDecoder[AppTask, A] = jsonOf[AppTask, A]

  val realm = "dummyrealm"

  val authStore: BasicAuthenticator[AppTask, String] = (creds: BasicCredentials) =>
    if (creds.username == "username" && creds.password == "password") RIO.some(creds.username)
    else RIO.none

  val basicAuth: AuthMiddleware[AppTask, String] = BasicAuth(realm, authStore)

  private def handleError(error: Throwable): AppTask[Response[AppTask]] =
    error match {
      case BadRequestError(msg)             => BadRequest(msg)
      case NoActiveTariffNotFoundError(msg) => NotFound(msg)
      case throwable                        => InternalServerError(throwable.getMessage)
    }

  private[route] val tariffRoutes =
    basicAuth(AuthedRoutes.of[String, AppTask] {
      case ctx @ POST -> Root / "tariff" as user =>
        for {
          tariff <- ctx.req.as[Tariff].tapError(t => log.error(t.getMessage))
          _ <- log.info(s"Handling request $tariff [$user]")
          resp <-
            TariffService
              .submitTariff(tariff)
              .foldM(handleError, _ => Created(s"A new tariff has been added $tariff"))
        } yield resp
    })

  private[route] val chargeSessionRoutes =
    HttpRoutes
      .of[AppTask] {
        case req @ POST -> Root / "session" =>
          for {
            chargeSession <- req.as[AddChargeSessionRequest].tapError(t => log.error(t.getMessage))
            _ <- log.info(s"Handling request $chargeSession")
            resp <-
              ChargeSessionService
                .addChargeSession(chargeSession)
                .foldM(handleError, _ => Created(s"A new charge session has been added $chargeSession"))
          } yield resp
      }

  val allRoutes: Kleisli[AppTask, Request[AppTask], Response[AppTask]] =
    (Routes.chargeSessionRoutes <+> Routes.tariffRoutes).orNotFound
}
