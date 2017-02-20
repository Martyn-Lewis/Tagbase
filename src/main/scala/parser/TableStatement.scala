package parser

import datatypes.{DatabasePool, Taggable}

abstract class TableStatement {
  def indented_print(indent: Int): Unit
  def generate_indexes(db: DatabasePool): Map[String, Double]
  def evaluate(db: DatabasePool): Iterator[Taggable]
}
