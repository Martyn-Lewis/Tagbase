package parser

import datatypes.{DatabasePool, SelectResponse, DatabaseIterator}

class DatabaseTableStatement(val database: String) extends TableStatement {
  override def toString: String = "database@" + database

  override def indented_print(indent: Int) = println(("\t" * indent) + "database reference: " +  database)

  override def evaluate[T](db: DatabasePool) = new SelectResponse[T](new DatabaseIterator(db.get_pool(database)).asInstanceOf[Iterator[T]])

  override def generate_indexes(db: DatabasePool) = db.get_pool(database).generate_weights()
}
