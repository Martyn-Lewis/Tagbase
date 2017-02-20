package parser

import scala.util.parsing.combinator.RegexParsers

class QueryParser extends RegexParsers {
  override val skipWhitespace = false
  val expressionParser: ExpressionParser = new ExpressionParser
  def KW_COUNT: Parser[String] = """COUNT""".r ^^ { _.toString }
  def KW_SELECT: Parser[String] = """SELECT""".r ^^ { _.toString }
  def KW_FROM: Parser[String] = """FROM""".r ^^ { _.toString }
  def KW_WITH: Parser[String] = """WITH""".r ^^ { _.toString }
  def TYPESTRING: Parser[String] = """[\*]""".r ^^ { _.toString } // TODO
  def DBSTRING: Parser[String] = """[a-zA-Z_0-9]+""".r ^^ { _.toString } // TODO
  def SEMICOLON: Parser[String] = """[;]""".r ^^ { _.toString }
  def NEWLINE: Parser[String] = """[\n]""".r
  def SUBEXPR: Parser[String] = """[']([0-9A-Za-z\-\_\(\)\|\&\?\: ]+)[']""".r ^^ {(x) => x.toString.slice(1, x.length - 1)}

  def TABLE: Parser[TableStatement] = (DBSTRING | ("(" ~> JOIN_STMT <~ ")")) ^^ {
    case s: Statement => new SubTableStatement(s)
    case s: String => new DatabaseTableStatement(s)
  }

  // SELECT typestring FROM tablestring WHERE 'query'
  def SELECT_FROM: Parser[SelectStatement] = (KW_SELECT <~ whiteSpace) ~ (TYPESTRING <~ whiteSpace) ~ (KW_FROM <~ whiteSpace) ~ TABLE ~ opt((whiteSpace ~> KW_WITH) ~ (whiteSpace ~> SUBEXPR)) ^^ {
    case kw_select ~ typestring ~ kw_from ~ table ~ opt => opt match {
      case Some(kw_with ~ expression) => new SelectStatement(table, typestring, expressionParser.parse(expressionParser.EXPRESSION, expression).get)
      case _ => new SelectStatement(table, typestring, new AlwaysExpression)
    }
  }

  // COUNT typestring FROM tablestring WHERE 'query'
  def COUNT_FROM: Parser[CountStatement] = (KW_COUNT <~ whiteSpace) ~ (TYPESTRING <~ whiteSpace) ~ (KW_FROM <~ whiteSpace) ~ TABLE ~ opt((whiteSpace ~> KW_WITH) ~ (whiteSpace ~> SUBEXPR)) ^^ {
    case kw_count ~ typestring ~ kw_from ~ table ~ option => option match {
      case Some(kw_with ~ expression) => new CountStatement(table, typestring, expressionParser.parse(expressionParser.EXPRESSION, expression).get)
      case _ => new CountStatement(table, typestring, new AlwaysExpression)
    }
  }

  // <SELECT> JOIN <SELECT | JOIN>
  def JOIN_STMT: Parser[Statement] = rep1sep(SELECT_FROM, whiteSpace ~ """JOIN""".r ~ whiteSpace) ^^ { case statements => if(statements.size > 1) new JoinStatement(statements) else statements.head }

  def STATEMENT: Parser[Statement] = opt(whiteSpace) ~> (JOIN_STMT | COUNT_FROM) <~ opt(NEWLINE)
  def QUERY: Parser[StatementCollection] = rep1sep(STATEMENT, opt(whiteSpace) ~ SEMICOLON) ^^ { case s => new StatementCollection(s) }
}