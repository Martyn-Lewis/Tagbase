package parser

import datatypes.{DatabasePool, DatabaseRow}

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

  override def evaluate(db: DatabasePool) = {
    val target = db.get_pool(database)
    for(obj <- objects) {
      val tags = obj.objects find (_._1 == "tags") match {
        case Some((_, s)) => s.substring(1, s.length - 1).split(",").map(_.trim)
        case None => throw new RuntimeException("(Rename this exception) No tags to insert with")
      }

      val values = obj.objects.filter(_._1 != "tags").map((x) => x._1 -> x._2).toMap
      target.add_element(new DatabaseRow(values, tags.toSet))
    }

    Iterator.empty
  }
}
