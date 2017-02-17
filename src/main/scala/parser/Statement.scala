package parser

import database_test.DatabasePool

abstract class Statement {
  def indented_print(indent: Int): Unit
  def evaluate(db: DatabasePool): Iterator[Taggable]
}
