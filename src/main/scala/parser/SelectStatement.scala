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

    def default = new SelectResponse[T](query.compile().evaluate_many(source.iterator.asInstanceOf[Iterator[Taggable]]).asInstanceOf[Iterator[T]])

    // TODO: Not this
    // Index support-ish
    database match {
      case d: DatabaseTableStatement =>
        query match {
          case q: AllExpression => // (a & b) = min(a, b)
            val tag = indexes.minBy(_._2)._1
            val pool = db.get_pool(d.database)
            if(pool.index_records.contains(tag))
              new SelectResponse[T](query.compile().evaluate_many(pool.index_records(tag).toIterator).asInstanceOf[Iterator[T]])
            else
              default
          case _ => default
        }
      case _ =>
        default
    }
  }
}
