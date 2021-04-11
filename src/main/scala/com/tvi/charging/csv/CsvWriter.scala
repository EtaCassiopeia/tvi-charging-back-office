package com.tvi.charging.csv

trait CsvWriter[A] {
  def write(a: A): String
}
