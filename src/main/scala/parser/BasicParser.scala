package parser

import datatypes.Database

import scala.annotation.tailrec
import scala.util.parsing.combinator._

class BasicParser extends RegexParsers {
  override val skipWhitespace = true
  def WORD: Parser[String] = """([A-Za-z][A-Za-z\-]+|[A-Za-z])""".r ^^ { _.toString }
  def MINUS: Parser[String] = """[-]""".r ^^ { _.toString }
  def PIPE: Parser[String] = """[|]""".r ^^ { _.toString }
  def AMPERSAND: Parser[String] = """[&]""".r ^^ { _.toString }
  def QUESTION: Parser[String] = """[?]""".r ^^ { _.toString }
  def COLON: Parser[String] = """[:]""".r ^^ { _.toString }
  def TAG: Parser[Tag] = WORD ^^ { Tag }
}