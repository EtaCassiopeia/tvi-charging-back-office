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

  def tariffService(): Kleisli[AppTask, Request[AppTask], Response[AppTask]] =
    basicAuth(AuthedRoutes.of[String, AppTask] {
      case ctx @ POST -> Root / "tariff" as user =>
        for {
          tariff <- ctx.req.as[Tariff].tapError(t => log.error(t.getMessage))
          _ <- log.info(s"Handling request $tariff [$user]")
          _ <- TariffService.submitTariff(tariff)
          resp <- Ok(tariff.toString)
        } yield resp
    }).orNotFound
}
