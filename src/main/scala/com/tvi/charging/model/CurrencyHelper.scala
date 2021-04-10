package com.tvi.charging.model

import cats.implicits._

import java.util.Currency
import scala.jdk.CollectionConverters._

object CurrencyHelper {
  private val availableCurrencies =
    Currency.getAvailableCurrencies.asScala.toList.map(currency => currency.getCurrencyCode -> currency).toMap

  def getCurrencyInstance(currencyCode: String): Either[String, Currency] = {
    if (currencyCode == null || currencyCode.isEmpty)
      "Currency code is empty".asLeft
    Either.fromOption(availableCurrencies.get(currencyCode), ifNone = s"Invalid currency code: `$currencyCode`")
  }
}
