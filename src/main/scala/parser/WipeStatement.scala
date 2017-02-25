package parser

import datatypes._

class WipeStatement(val database: String) extends Statement {
  override def toString: String = "DELETE FROM " + database
  override def indented_print(indent: Int) = {
    val offset = "\t" * indent
    val offsetp1 = "\t" * (indent + 1)
    println(offset + "Wipe statement:")
    println(offsetp1 + "target: " + database)
  }

  override def evaluate[T](db: DatabasePool): DatabaseResponse = {
    new WipeResponse(database)
  }
}
