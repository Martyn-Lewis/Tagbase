package parser

import datatypes.{DatabasePool, DatabaseResponse, Taggable}

abstract class Statement {
  def indented_print(indent: Int): Unit
  def evaluate[T](db: DatabasePool): DatabaseResponse
}
