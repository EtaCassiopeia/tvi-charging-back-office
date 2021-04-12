package com.tvi.charging.route

import cats.data.Kleisli
import cats.implicits._
import com.tvi.charging.ChargingService.AppTask
import com.tvi.charging.model._
import com.tvi.charging.repository.{ChargeSessionService, TariffService}
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.AuthMiddleware
import org.http4s.server.middleware.authentication.BasicAuth
import org.http4s.server.middleware.authentication.BasicAuth.BasicAuthenticator
import zio.RIO
import zio.interop.catz._
import zio.logging.log
import com.tvi.charging.model.implicits._
import io.circe.refined._

object Routes {

  private val dsl = Http4sDsl[AppTask]
  import dsl._

  val realm = "dummyrealm"

  val authStore: BasicAuthenticator[AppTask, String] = (creds: BasicCredentials) =>
    if (creds.username == "username" && creds.password == "password") RIO.some(creds.username)
    else RIO.none

  val basicAuth: AuthMiddleware[AppTask, String] = BasicAuth(realm, authStore)

  private def handleError(error: Throwable): AppTask[Response[AppTask]] =
    error match {
      case BadRequestError(msg)             => BadRequest(HttpError(msg))
      case NoActiveTariffNotFoundError(msg) => NotFound(HttpError(msg))
      case throwable                        => InternalServerError(HttpError(s"${throwable.getClass.getSimpleName} ${throwable.getMessage}"))
    }

  private[route] val tariffRoutes =
    basicAuth(AuthedRoutes.of[String, AppTask] {
      case ContextRequest(user, req @ POST -> Root / "tariff") =>
        for {
          tariff <- req.as[AddTariffRequest].tapError(t => log.error(t.getMessage))
          _ <- log.info(s"Handling request $tariff [$user]")
          resp <-
            TariffService
              .submitTariff(tariff)
              .foldM(
                handleError,
                addedTariff => Created(HttpCreatedResponse(s"A new tariff has been added $addedTariff"))
              )
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
                .foldM(
                  handleError,
                  _ => Created(HttpCreatedResponse(s"A new charge session has been added $chargeSession"))
                )
          } yield resp
      }

  val allRoutes: Kleisli[AppTask, Request[AppTask], Response[AppTask]] =
    (Routes.chargeSessionRoutes <+> DriverRoutes.routes <+> Routes.tariffRoutes).orNotFound
}
