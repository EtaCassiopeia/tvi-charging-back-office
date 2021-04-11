package com.tvi.charging.model

case class NoActiveTariffNotFoundError(errorMessage: String) extends Throwable(errorMessage)
case class BadRequestError(errorMessage: String) extends Throwable(errorMessage)

case class ParseError(errorMessage: String) extends Throwable(errorMessage)
case class GenericInternalError(errorMessage: String) extends Throwable(errorMessage)

case class HttpError(error: String)
