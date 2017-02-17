package database_test

import parser.{ExpressionParser, Taggable}

object profiler extends scala.App {

  class Datatype(contents: String, tags_to_use: Set[String]) extends Taggable {
    override var tags: Set[String] = tags_to_use
  }

  val test_parser = new ExpressionParser()
  var data: List[Datatype] = List()

  println("Constructing dataset")
  val construct_start = System.nanoTime()

  var predata: List[Datatype] = List()
  predata ::= new Datatype("d1", Set("a"))
  predata ::= new Datatype("d2", Set("a", "b"))
  predata ::= new Datatype("d3", Set("a", "b", "c"))
  predata ::= new Datatype("d4", Set("a", "b", "c", "d"))
  predata ::= new Datatype("d5", Set("a", "b", "c", "d", "e"))
  predata ::= new Datatype("d6", Set("a", "b", "c", "d", "e", "f"))

  data :::= predata
  for (i <- 1 to 16) {
    data :::= data
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

  val query = "a ? (b (c|g)) : (c (d|(e f)))"
  println("Evaluating query: " + query)

  val parsed = test_parser.parse(test_parser.EXPRESSION, query).get
  val optimised = parsed.full_optimise(index_weights)
  val compiled = optimised.compile()
  println("Unoptimised: " + parsed.toString)
  println("Optimised: " + optimised.toString)

  var threads: List[Thread] = List()
  val thread_count: Long = Runtime.getRuntime().availableProcessors()
  val iterations: Long = 64

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
          println("Evaluation completed in " + (evaluate_end - evaluate_start) / 1000000.0 + "ms - " + (data.size.asInstanceOf[Float] / seconds) + " items per second - " + result + " matches")
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
