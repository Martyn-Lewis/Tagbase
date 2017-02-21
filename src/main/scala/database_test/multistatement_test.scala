package database_test

import datatypes.{DatabasePool, Taggable, DatabaseRow}
import parser.{QueryParser, StatementCollection}

object multistatement_test extends scala.App {
  // Note: Currently the statement has too much power over the database.
  // It would perhaps be much saner though slower to have the statement yield transactions or similar instead.

  var db = new DatabasePool
  val parser = new QueryParser

  def execute_statement(stmt: String) = {
    println("Executing statement: " + stmt)
    val parsed_insert = parser.parseAll(parser.QUERY, stmt)
    if(parsed_insert.successful) {
      val target = parsed_insert.get
      /*target.indented_print(0)
      println(target.toString)*/
      target.statements.foreach ((x) => {
        val results = x.evaluate(db)
        for(result <- results) {
          println(s"Database result: (${result.tags.map(_.toString).mkString(", ")}) ${result.asInstanceOf[DatabaseRow].contents}")
        }
      })
    } else {
      println("Failed to parse a statement:")
      println(parsed_insert.toString)
    }
  }

  var pool1 = db.create_pool("db1")
  var pool2 = db.create_pool("db2")
  var directory = db.create_pool("my_directory")
  var pool3 = db.create_pool("some_database")

  execute_statement("""INSERT INTO db1 VALUES {tags="a, b, c", value="Test row 1"}, {tags="a, b, c", value="Test row 2"}""")
  execute_statement("""INSERT INTO db2 VALUES {tags="a, b, c", value="Test row 3"}, {tags="a, b, c", value="Test row 4"}""")
  execute_statement("""SELECT * FROM (SELECT * FROM db1 WITH 'a' JOIN SELECT * FROM db2 WITH 'b') WITH 'a b'""")

  execute_statement("""INSERT INTO my_directory VALUES {tags="photos, kittens", path="kitty.jpg", attributes="read-only"}""")
  execute_statement("""INSERT INTO my_directory VALUES {tags="photos, dogs", path="puppy.jpg", attributes="read-only"}""")
  execute_statement("""SELECT * FROM my_directory WITH 'photos'""")

  for(p <- List(pool1, pool2, pool3)) {
    p.add_element(new DatabaseRow(Map("contents" -> "d1"), Set("a")))
    p.add_element(new DatabaseRow(Map("contents" -> "d2"), Set("a", "b")))
    p.add_element(new DatabaseRow(Map("contents" -> "d3"), Set("a", "b", "c")))
    p.add_element(new DatabaseRow(Map("contents" -> "d4"), Set("a", "b", "c", "d")))
    p.add_element(new DatabaseRow(Map("contents" -> "d5"), Set("a", "b", "c", "d", "e")))
    p.add_element(new DatabaseRow(Map("contents" -> "d6"), Set("a", "b", "c", "d", "e", "f")))
  }

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
