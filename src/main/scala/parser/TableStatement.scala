package parser

import datatypes.{DatabasePool, DatabaseResponse, SelectResponse, Taggable}

abstract class TableStatement {
  def indented_print(indent: Int): Unit
  def generate_indexes(db: DatabasePool): Map[String, Double]
  def evaluate[T](db: DatabasePool): SelectResponse[T]
}
