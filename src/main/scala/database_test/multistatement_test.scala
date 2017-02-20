package database_test

import datatypes.{DatabasePool, Taggable}
import parser.{QueryParser, StatementCollection}

object multistatement_test extends scala.App {
  // Note: Currently the statement has too much power over the database.
  // It would perhaps be much saner though slower to have the statement yield transactions or similar instead.

  class Datatype(val contents: String, tags_to_use: Set[String]) extends Taggable {
    override var tags: Set[String] = tags_to_use
  }

  val parser = new QueryParser
  val query = "SELECT * FROM (SELECT * FROM db1 WITH 'a' JOIN SELECT * FROM db2 WITH 'b') WITH 'a b'"
  println("Parsing query: " + query)
  val parsed: StatementCollection = parser.parseAll(parser.QUERY, query).get
  parsed.indented_print(0)
  println(parsed.toString)
  var db = new DatabasePool
  var pool1 = db.create_pool("db1")
  var pool2 = db.create_pool("db2")
  var pool3 = db.create_pool("some_database")

  for(p <- List(pool1, pool2, pool3)) {
    p.add_element(new Datatype("d1", Set("a")))
    p.add_element(new Datatype("d2", Set("a", "b")))
    p.add_element(new Datatype("d3", Set("a", "b", "c")))
    p.add_element(new Datatype("d4", Set("a", "b", "c", "d")))
    p.add_element(new Datatype("d5", Set("a", "b", "c", "d", "e")))
    p.add_element(new Datatype("d6", Set("a", "b", "c", "d", "e", "f")))
  }

  val results: Iterator[Datatype] = (parsed.statements.head).evaluate(db).asInstanceOf[Iterator[Datatype]]
  results foreach ((x) => {
    println(x.contents)
  })
}
