package parser

import datatypes.{DatabasePool, DatabaseResponse, SelectResponse, Taggable}

class SelectStatement(val database: TableStatement, val typestring: List[String], val expression: Expression) extends Statement {
  override def toString: String = "SELECT " + typestring + " FROM " + database + " WITH " + expression.toString
  override def indented_print(indent: Int) = {
    val offset = "\t" * indent
    val offsetp1 = "\t" * (indent + 1)
    println(offset + "Select statement:")
    println(offsetp1 + "source: ")
    database.indented_print(indent + 2)
    println(offsetp1 + "typestring: " + typestring.mkString(", "))
    println(offsetp1 + "expression: " + expression.toString)
  }

  override def evaluate[T](db: DatabasePool): DatabaseResponse = {
    val source = database.evaluate[T](db)
    val indexes = database.generate_indexes(db)
    val query = expression.full_optimise(indexes)

    new SelectResponse[T](query.compile().evaluate_many(source.iterator.asInstanceOf[Iterator[Taggable]]).asInstanceOf[Iterator[T]])
  }
}
