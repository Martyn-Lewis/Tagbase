package parser

import datatypes._

class DeleteStatement(val database: String, val expression: Expression) extends Statement {
  override def toString: String = "DELETE FROM " + database + " WITH " + expression.toString
  override def indented_print(indent: Int) = {
    val offset = "\t" * indent
    val offsetp1 = "\t" * (indent + 1)
    println(offset + "Delete statement:")
    println(offsetp1 + "target: " + database)
    println(offsetp1 + "expression: " + expression.toString)
  }

  override def evaluate[T](db: DatabasePool): DatabaseResponse = {
    new DeleteResponse(database, expression.full_optimise(db.get_pool(database).generate_weights()).compile_method().asInstanceOf[(DatabaseRow => Boolean)])
  }
}
