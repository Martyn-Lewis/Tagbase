package parser

import org.scalatest.FunSuite

class Data(contents: String, data_tags: Set[String]) extends Taggable {
  override var tags: Set[String] = data_tags
}

class ParserSuite extends FunSuite {
  import parser.ExpressionParser

  val test_parser: ExpressionParser = new ExpressionParser()
  val query_parser: QueryParser = new QueryParser
  val elements: List[Data] = List(
    new Data("Hello", Set("a")),
    new Data("world", Set("a", "b")),
    new Data("test", Set("a", "b", "c"))
  )

  test("a b is (b&a)") {
    assert((test_parser.parse(test_parser.EXPRESSION, "a b").get.asInstanceOf[AllExpression].children map (_.toString)) == Set("b", "a"))
  }

  test("a & b is (a b)") {
    assert(test_parser.parse(test_parser.EXPRESSION, "a & b").get.toString == test_parser.parse(test_parser.EXPRESSION, "a b").get.toString)
  }

  test("a b c d is (a&b&d&c)") {
    assert((test_parser.parse(test_parser.EXPRESSION, "a b c d").get.asInstanceOf[AllExpression].children map (_.toString)) == Set("d", "c", "b", "a"))
  }

  test("(a b) (a c) is (a&b&c)") {
    assert((test_parser.parse(test_parser.EXPRESSION, "(a b) (a c)").get.asInstanceOf[AllExpression].children map (_.toString)) == Set("c", "b", "a"))
  }

  test("a | b c is (a|(c&b))") {
    assert(test_parser.parse(test_parser.EXPRESSION, "a | b c").get.toString == "(a | (c & b))")
  }

  test("(a b) ? (c | d) : (e f) is (b&a)?(c|d):(f&e)") {
    assert(test_parser.parse(test_parser.EXPRESSION, "(a b) ? (c | d) : (e f)").get.toString == "(b & a) ? (c | d) : (f & e)")
  }

  test("--a is a") {
    assert(test_parser.parse(test_parser.EXPRESSION, "--a").get.toString == "a")
  }

  test("-(a b) ? -(c | d) : -(e f) is -(b&a)?-(c|d):-(f&e)") {
    assert(test_parser.parse(test_parser.EXPRESSION, "-(a b) ? -(c | d) : -(e f)").get.toString == "-(b & a) ? -(c | d) : -(f & e)")
  }

  test("optimised a | g with a-b is a") {
    assert(test_parser.parse(test_parser.EXPRESSION, "a | g").get.full_optimise(Map[String, Double]("a" -> 0.5, "b" -> 0.5)).toString == "a")
  }

  test("a | b | c | d is (a | b | c | d)") {
    assert(test_parser.parse(test_parser.EXPRESSION, "a | b | c | d").get.full_optimise(Map[String, Double]("a" -> 0.5, "b" -> 0.5, "c" -> 0.5, "d" -> 0.5)).toString == "(d | c | b | a)")
  }

  test("Match test #1") {
    def reference(t: Taggable): Boolean = (t.tags intersect Set("a", "b")).size == 2
    val compiled = test_parser.parse(test_parser.EXPRESSION, "a b").get.compile_method()
    assert((elements filter compiled) == (elements filter reference))
  }

  test("SELECT test #1") {
    val parsed: SelectStatement = query_parser.parse(query_parser.STATEMENT, "SELECT * FROM test_database WITH a b").get.asInstanceOf[SelectStatement]
    assert(parsed.database.toString == "test_database")
    assert(parsed.typestring == "*")
    assert(parsed.expression.toString == "(b & a)")
  }

  test("COUNT test #1") {
    val parsed: CountStatement = query_parser.parse(query_parser.STATEMENT, "COUNT * FROM test_database WITH a b").get.asInstanceOf[CountStatement]
    assert(parsed.database.toString == "test_database")
    assert(parsed.expression.toString == "(b & a)")
    assert(parsed.typestring == "*")
  }
}