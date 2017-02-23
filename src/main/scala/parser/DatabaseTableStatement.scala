package parser

import datatypes.{DatabasePool, SelectResponse}

class DatabaseTableStatement(database: String) extends TableStatement {
  override def toString: String = "database@" + database

  override def indented_print(indent: Int) = println(("\t" * indent) + "database reference: " +  database)

  override def evaluate[T](db: DatabasePool) = new SelectResponse[T](db.get_pool(database).elements.toIterator.asInstanceOf[Iterator[T]])

  override def generate_indexes(db: DatabasePool) = db.get_pool(database).generate_weights()
}
