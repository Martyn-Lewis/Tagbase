package parser

import datatypes.DatabasePool

class SubTableStatement(statement: Statement) extends TableStatement {
  override def toString: String = "query@(" + statement.toString + ")"
  override def indented_print(indent: Int) = {
    println(("\t" * indent) + "subquery:")
    statement.indented_print(indent + 1)
  }

  override def evaluate(db: DatabasePool) = statement.evaluate(db)

  override def generate_indexes(db: DatabasePool) = statement match {
    case s: SelectStatement => s.database match {
      case d: DatabaseTableStatement => d.generate_indexes(db)
      case d: SubTableStatement => d.generate_indexes(db)
      case _ => throw new RuntimeException("This also should be impossible.")
    }
    case s: JoinStatement => (s.queries.map (_.asInstanceOf[SelectStatement].database.generate_indexes(db))) reduceLeft((l: Map[String, Double], r: Map[String, Double]) => l ++ r.map({ case (k, v) => k -> (v + l.getOrElse(k, 0.0))}))
    case _ => throw new RuntimeException("This should be impossible.")
  }
}
