package com.tvi.charging.model

import io.circe.generic.auto._
import io.circe.parser.decode
import zio.ZIO
import zio.test.Assertion.equalTo
import zio.test.{DefaultRunnableSpec, assert}

import java.time.LocalDateTime

object LocalDateTimeDecoderSpec extends DefaultRunnableSpec {

  override def spec =
    suite("LocalDateTimeDecoder")(
      testM("should be able to decode a date with ISO_OFFSET_DATE_TIME format") {
        case class WithDate(dateTime: LocalDateTime)
        for {
          res <- ZIO.fromEither(decode[WithDate]("""{"dateTime":"2018-01-19T16:39:57-08:00"}"""))
        } yield assert(res)(equalTo(WithDate(LocalDateTime.of(2018, 1, 19, 16, 39, 57, 0))))
      },
      testM("should be able to decode a date with ISO_INSTANT format") {
        case class WithDate(dateTime: LocalDateTime)
        for {
          res <- ZIO.fromEither(decode[WithDate]("""{"dateTime":"2018-01-19T16:39:57Z"}"""))
        } yield assert(res)(equalTo(WithDate(LocalDateTime.of(2018, 1, 19, 16, 39, 57))))
      }
    )
}
