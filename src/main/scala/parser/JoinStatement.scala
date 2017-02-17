package parser

import database_test.DatabasePool

class JoinStatement(val queries: List[Statement]) extends Statement {
  override def toString: String = queries map ("(" + _.toString + ")") mkString " JOIN "

  override def indented_print(indent: Int) = {
    println(("\t" * indent) + "join:")
    queries foreach (_.indented_print(indent + 1))
  }

  override def evaluate(db: DatabasePool) = queries.flatMap(_.evaluate(db)).toIterator
}
