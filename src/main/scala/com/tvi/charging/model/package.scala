package com.tvi.charging

import io.circe.{Decoder, Encoder}

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId, ZoneOffset, ZonedDateTime}
import scala.util.Try

package object model {
  object implicits {
    final def decodeLocalDateTime(formatter: DateTimeFormatter): Decoder[LocalDateTime] =
      Decoder[String].emapTry(str => Try(ZonedDateTime.parse(str, formatter).toLocalDateTime))

    final def encodeLocalDateTime(formatter: DateTimeFormatter): Encoder[LocalDateTime] =
      Encoder[String].contramap(ZonedDateTime.of(_, ZoneOffset.UTC).format(formatter))

    implicit final val decodeLocalDateTimeDefault: Decoder[LocalDateTime] =
      decodeLocalDateTime(DateTimeFormatter.ISO_OFFSET_DATE_TIME).or(decodeLocalDateTime(DateTimeFormatter.ISO_INSTANT))
    implicit final val encodeLocalDateTimeDefault: Encoder[LocalDateTime] = encodeLocalDateTime(
      DateTimeFormatter.ISO_INSTANT
    )
  }
}
