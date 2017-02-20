package database_test

import datatypes.{DatabasePool, Taggable, DatabaseRow}
import parser.{QueryParser, StatementCollection}

object multistatement_test extends scala.App {
  // Note: Currently the statement has too much power over the database.
  // It would perhaps be much saner though slower to have the statement yield transactions or similar instead.

  var db = new DatabasePool
  var pool1 = db.create_pool("db1")
  var pool2 = db.create_pool("db2")
  var pool3 = db.create_pool("some_database")

  for(p <- List(pool1, pool2, pool3)) {
    p.add_element(new DatabaseRow("d1", Set("a")))
    p.add_element(new DatabaseRow("d2", Set("a", "b")))
    p.add_element(new DatabaseRow("d3", Set("a", "b", "c")))
    p.add_element(new DatabaseRow("d4", Set("a", "b", "c", "d")))
    p.add_element(new DatabaseRow("d5", Set("a", "b", "c", "d", "e")))
    p.add_element(new DatabaseRow("d6", Set("a", "b", "c", "d", "e", "f")))
  }

  val parser = new QueryParser

  // Insert test
  val insert_query = """INSERT INTO db1 VALUES {tags="a, b, c", value="Test row 1"}, {tags="a, b, c", value="Test row 2"}"""
  println("Parsing query: " + insert_query)

  val parsed_insert = parser.parseAll(parser.QUERY, insert_query)
  if(parsed_insert.successful) {
    val target = parsed_insert.get
    target.indented_print(0)
    println(target.toString)
    target.statements.head.evaluate(db)
  } else {
    println("Failed to parse insert statement")
    println(parsed_insert.toString)
  }

  // Select test
  val query = "SELECT * FROM (SELECT * FROM db1 WITH 'a' JOIN SELECT * FROM db2 WITH 'b') WITH 'a b'"
  println("Parsing query: " + query)
  val parsed: StatementCollection = parser.parseAll(parser.QUERY, query).get
  parsed.indented_print(0)
  println(parsed.toString)

  val results: Iterator[DatabaseRow] = (parsed.statements.head).evaluate(db).asInstanceOf[Iterator[DatabaseRow]]
  results foreach ((x) => {
    println(x.contents)
  })
}
