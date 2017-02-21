package parser

import datatypes.DatabasePool

class CountStatement(val database: TableStatement, val typestring: List[String], val expression: Expression) extends Statement {
  override def toString: String = "COUNT " + typestring + " FROM " + database + " WITH " + expression.toString

  override def indented_print(indent: Int) = {
    val offset = "\t" * indent
    val offsetp1 = "\t" * (indent + 1)
    println(offset + "Count statement:")
    println(offsetp1 + "source: ")
    database.indented_print(indent + 2)
    println(offsetp1 + "typestring: " + typestring.mkString(", "))
    println(offsetp1 + "expression: " + expression.toString)
  }

  override def evaluate(db: DatabasePool) = throw new RuntimeException("Don't evaluate a count like this.")
}
