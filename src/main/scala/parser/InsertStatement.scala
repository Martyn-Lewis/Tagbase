package parser

import datatypes.{DatabasePool, DatabaseRow, InsertResponse}

class InsertStatement(val database: String, val objects: List[QueryObject]) extends Statement {
  override def toString: String = "INSERT INTO " + database + " VALUES " + (objects map (_.toString) mkString ", ")

  override def indented_print(indent: Int) = {
    val offset = "\t" * indent
    val offsetp1 = "\t" * (indent + 1)
    val offsetp2 = "\t" * (indent + 2)
    println(offset + "Insert statement:")
    println(offsetp1 + "Target: ")
    println(offsetp2 + database)
    println(offsetp1 + "Values: ")
    for(obj <- objects) {
      println(offsetp2 + "Object: " + obj.toString)
    }
  }

  override def evaluate[T](db: DatabasePool) = {
    val target = db.get_pool(database)

    new InsertResponse(database, objects)
  }
}
