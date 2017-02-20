package parser

import datatypes.{DatabasePool, Taggable}

abstract class Statement {
  def indented_print(indent: Int): Unit
  def evaluate(db: DatabasePool): Iterator[Taggable]
}
