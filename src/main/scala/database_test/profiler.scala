package database_test

import java.lang

import datatypes._
import parser.{AllExpression, ExpressionParser, QueryParser, TagExpression}

import scala.collection.mutable.ArrayBuffer

object nuprofiler extends scala.App {
  val datapool = new DatabasePool
  val parser = new QueryParser

  def timeit(times: Int)(f: (() => Unit)): Double = {
    (1 to times).par.map((i) => {
      val evaluate_start = System.nanoTime().asInstanceOf[Double]
      f()
      val evaluate_end = System.nanoTime().asInstanceOf[Double]
      val distance = evaluate_end - evaluate_start
      val millis = distance / (1000 * 1000)
      println(s"Cycle $i took $millis milliseconds")
      millis
    }).sum / times
  }

  def evaluate_response(response: DatabaseResponse): Option[Iterator[DatabaseRow]] = response match {
    case resp: CountResponse =>
      println("Count:")
      resp.results foreach ((x) => {
        println(x._1.toString + " has " + x._2.toString + " elements")
      })
      None
    case resp: SelectResponse[DatabaseRow] =>
      var i = 0
      Some(resp.iterator)
    case resp: InsertResponse =>
      resp.inserts.foreach((x) => {
        val as_map = x.objects.toMap
        val pool = datapool.get_pool(resp.database)
        pool.add_element(new DatabaseRow(as_map.filter(_._1 != "tags"), as_map("tags").split(",").map(_.stripPrefix("\"").stripSuffix("\"").trim).toSet))
      })
      None
    case resp: MultipleResponse =>
      resp.responses.foreach(evaluate_response)
      None
  }

  def execute_statement(stmt: String): Unit = {
    val parsed_insert = parser.parseAll(parser.QUERY, stmt)
    if (parsed_insert.successful) {
      val target = parsed_insert.get
      evaluate_response(target.evaluate[DatabaseRow](datapool))
    } else {
      println("Failed to parse a statement:")
      println(parsed_insert.toString)
      throw new RuntimeException("Failed to parse a statement")
    }
  }

  datapool.create_pool("insert_test")

  val insert_tests = 4000
  val insert_query = """INSERT INTO insert_test VALUES {tags="a, b, c", value="Test row 1"}, {tags="a, b", value="Test row 2"}, {tags="a", value="Test row 3"}, {tags="a, b, c, d", value="Test row 4"}"""

  val distance = timeit(10)(() => {
    for (i <- 0 until insert_tests) {
      execute_statement(insert_query)
    }
  })

  println(s"$insert_tests insertion statements (${insert_tests * 4} values) took ${distance} milliseconds on average")

  val preparsed = parser.parseAll(parser.QUERY, insert_query).get
  var preparsed_distance: Double = 0.0
  val total = timeit(1)(() => {
    preparsed_distance = timeit(100)(() => {
      for (i <- 1 to insert_tests) {
        evaluate_response(preparsed.evaluate[DatabaseRow](datapool))
      }
    })
  })

  println(s"$insert_tests preparsed insertion statements (${insert_tests * 4} values) took ${preparsed_distance} milliseconds on average ($total total)")

  // Select

  val select_query = """SELECT * FROM (SELECT * FROM insert_test JOIN SELECT * FROM insert_test JOIN SELECT * FROM insert_test JOIN SELECT * FROM insert_test JOIN SELECT * FROM insert_test) WITH 'a c'"""

  val select_distance = timeit(32)(() => {
    val parsed_insert = parser.parseAll(parser.QUERY, select_query).get
    println("Determined size: " + evaluate_response(parsed_insert.evaluate[DatabaseRow](datapool).responses.head).get.size.toString)
  })

  println(s"${datapool.get_pool("insert_test").head_chunk.calculate_size() * 5} selects took an average of ${select_distance} milliseconds")
}

object profiler extends scala.App {
  class Datatype(contents: String, tags_to_use: Set[String]) extends Taggable {
    override var tags: Set[String] = tags_to_use
  }

  val test_parser = new ExpressionParser()
  var data: ArrayBuffer[Datatype] = ArrayBuffer.empty[Datatype]

  println("Constructing dataset")
  val construct_start = System.nanoTime()

  var predata: ArrayBuffer[Datatype] = ArrayBuffer.empty[Datatype]
  var indexes: collection.mutable.Map[String, ArrayBuffer[Datatype]] = collection.mutable.Map[String, ArrayBuffer[Datatype]]()
  for(k <- List("a", "b", "c", "d", "e", "f")) {
    indexes(k) = ArrayBuffer.empty[Datatype]
  }
  predata += new Datatype("d1", Set("a"))
  indexes("a") += predata(0)
  predata += new Datatype("d2", Set("a", "b"))
  indexes("a") += predata(1)
  indexes("b") += predata(1)
  predata += new Datatype("d3", Set("a", "b", "c"))
  indexes("a") += predata(2)
  indexes("b") += predata(2)
  indexes("c") += predata(2)
  predata += new Datatype("d4", Set("a", "b", "c", "d"))
  indexes("a") += predata(3)
  indexes("b") += predata(3)
  indexes("c") += predata(3)
  indexes("d") += predata(3)
  predata += new Datatype("d5", Set("a", "b", "c", "d", "e"))
  indexes("a") += predata(4)
  indexes("b") += predata(4)
  indexes("c") += predata(4)
  indexes("d") += predata(4)
  indexes("e") += predata(4)
  predata += new Datatype("d6", Set("a", "b", "c", "d", "e", "f"))
  indexes("a") += predata(5)
  indexes("b") += predata(5)
  indexes("c") += predata(5)
  indexes("d") += predata(5)
  indexes("e") += predata(5)
  indexes("f") += predata(5)

  data ++= predata
  for (i <- 1 to 20) {
    data ++=  data
    for(k <- indexes.keys) {
      indexes(k) ++= indexes(k)
    }
  }

  // index_weights are how often each tag occurs in the database.
  // This will be used for optimisations in the following ways:
  //  Any occurrence of tag x where weight(x) is 1, is replaced with always
  //  Any occurrence of tag x where weight(x) is 0 (or it has no weight at all), is replaced with never
  //  The order of execution is based on the weight(x) and the complexity(sum of weight and complexity in an expression)
  //    For example:
  //      where a = 1.0 and b = 0.5
  //      a & b is slower than b & a since it will waste twice as many operations doing nothing.
  //      Though in this particular case, since a is 1, it will evaluate as
  //      always & b = b
  //    Simply put:
  //      A higher weight means that, on average, the given tag or expression will yield results more often.
  //      With an or, it therefore makes sense to put those on the left hand so they're evaluated less.
  //      With an and, it therefore makes sense to put those on the right hand so they're ignored more.
  val index_weights = Map[String, Double]("a" -> 6.0/6.0, "b" -> 5.0/6.0, "c" -> 4.0/6.0, "d" -> 3.0/6.0, "e" -> 2.0/6.0, "f" -> 1.0/6.0)

  val construct_end = System.nanoTime()
  println("Construction completed in " + (construct_end - construct_start) / 1000000 + "ms")

  val query = "((a b) | (g f)) ? f c : k m"
  println("Evaluating query: " + query)

  val parsed = test_parser.parse(test_parser.EXPRESSION, query).get
  val optimised = parsed.full_optimise(index_weights)
  val compiled = optimised.compile()
  println("Unoptimised: " + parsed.toString)
  println("Optimised: " + optimised.toString)

  var threads: List[Thread] = List()
  val thread_count: Long = Runtime.getRuntime().availableProcessors()
  val iterations: Long = 64

  val source = optimised match {
    case src: AllExpression => indexes(src.children.min(Ordering.by((k: TagExpression) => index_weights(k.tag.word))).tag.word)
    case src => data
  }

  for (i <- 1 to thread_count.toInt) {
    threads ::= new Thread(new Runnable {
      override def run() = {
        for (iteration <- 1 to iterations.toInt) {
          val evaluate_start = System.nanoTime().asInstanceOf[Double]

          //val result = (data filter compiled).toList
          var result = 0

          result = compiled.evaluate_many(data.toIterator).size

          val evaluate_end = System.nanoTime().asInstanceOf[Double]
          val seconds: Double = (evaluate_end - evaluate_start) / 1000000000.0
          println(s"(Thread $i) Evaluation completed in " + (evaluate_end - evaluate_start) / 1000000.0 + "ms - " + (data.size.asInstanceOf[Float] / seconds) + " items per second - " + result + " matches")
        }
      }
    })
  }

  val evaluate_start = System.nanoTime().asInstanceOf[Double]
  threads foreach (_.start())
  threads foreach (_.join())
  val evaluate_end = System.nanoTime().asInstanceOf[Double]

  val seconds: Double = (evaluate_end - evaluate_start) / 1000000000.0
  val items_per: Long = data.size * iterations * thread_count

  println(items_per + " elements processed in " + seconds + " seconds - " + (items_per / seconds) + " items per second")
}
