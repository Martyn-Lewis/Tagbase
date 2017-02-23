package parser

import datatypes.{DatabasePool, MultipleResponse}

class StatementCollection(val statements: List[Statement]) extends Statement {
  override def toString: String = statements map (_.toString) mkString "; "

  override def indented_print(indent: Int) = {
    println(("\t" * indent) + "Multiple statements: ")
    statements foreach (_.indented_print(indent + 1))
  }

  override def evaluate[T](db: DatabasePool) = new MultipleResponse(statements.map(_.evaluate[T](db)))
}
