package parser

import datatypes.{DatabasePool, SelectResponse, MultipleResponse}

class JoinStatement(val queries: List[Statement]) extends Statement {
  override def toString: String = queries map ("(" + _.toString + ")") mkString " JOIN "

  override def indented_print(indent: Int) = {
    println(("\t" * indent) + "join:")
    queries foreach (_.indented_print(indent + 1))
  }

  override def evaluate[T](db: DatabasePool) = new MultipleResponse(queries.map(_.evaluate[T](db)))
}
