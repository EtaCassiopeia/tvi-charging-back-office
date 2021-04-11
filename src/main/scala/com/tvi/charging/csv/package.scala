package com.tvi.charging

package object csv {
  implicit class CsvOps[T <: Product](product: Product) {
    def toCsvRecord: String = {
      val productIterator = product.productIterator
      productIterator.toList.map(_.toString).mkString(",")
    }
  }

  def toCsv[A <: Product](records: List[A]): String = {
    val productFields = records.head.productElementNames
    val header = productFields.mkString("", ",", System.lineSeparator())
    header + records.map(_.toCsvRecord).mkString(System.lineSeparator())
  }
}
