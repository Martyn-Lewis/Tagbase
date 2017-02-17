package parser

import database_test.DatabasePool

class SelectStatement(val database: TableStatement, val typestring: String, val expression: Expression) extends Statement {
  override def toString: String = "SELECT " + typestring + " FROM " + database + " WITH " + expression.toString
  override def indented_print(indent: Int) = {
    val offset = "\t" * indent
    val offsetp1 = "\t" * (indent + 1)
    println(offset + "Select statement:")
    println(offsetp1 + "source: ")
    database.indented_print(indent + 2)
    println(offsetp1 + "typestring: " + typestring)
    println(offsetp1 + "expression: " + expression.toString)
  }

  override def evaluate(db: DatabasePool) = {
    val source = database.evaluate(db)
    val indexes = database.generate_indexes(db)
    val query = expression.full_optimise(indexes)

    query.compile().evaluate_many(source)
  }
}
