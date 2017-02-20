package parser

import scala.util.matching.Regex.Match
import scala.util.parsing.combinator.RegexParsers

class QueryParser extends RegexParsers {
  override val skipWhitespace = false
  val expressionParser: ExpressionParser = new ExpressionParser
  def KW_COUNT: Parser[String] = """COUNT""".r ^^ { _.toString }
  def KW_SELECT: Parser[String] = """SELECT""".r ^^ { _.toString }
  def KW_FROM: Parser[String] = """FROM""".r ^^ { _.toString }
  def KW_WITH: Parser[String] = """WITH""".r ^^ { _.toString }
  def KW_INSERT: Parser[String] = """INSERT""".r ~ whiteSpace ~ """INTO""".r ^^ { case l ~ _ ~ r => l + " " + r }
  def KW_VALUES: Parser[String] = """VALUES""".r ^^ { _.toString }
  def TYPESTRING: Parser[String] = """[\*]""".r ^^ { _.toString } // TODO
  def DBSTRING: Parser[String] = """[a-zA-Z_0-9]+""".r ^^ { _.toString } // TODO
  def SEMICOLON: Parser[String] = """[;]""".r ^^ { _.toString }
  def NEWLINE: Parser[String] = """[\n]""".r
  def SUBEXPR: Parser[String] = """[']([0-9A-Za-z\-\_\(\)\|\&\?\: ]+)[']""".r ^^ {(x) => x.toString.slice(1, x.length - 1)}

  def TABLE: Parser[TableStatement] = (DBSTRING | ("(" ~> JOIN_STMT <~ ")")) ^^ {
    case s: Statement => new SubTableStatement(s)
    case s: String => new DatabaseTableStatement(s)
  }

  def STRING: Parser[String] = """["]([\\]["]|[^"])+["]""".r ^^ {
    case s: String =>
      """[\\][brnt"]""".r.replaceAllIn(s, m => m.group(0) match {
        case "\\n" => "\n"
        case "\\b" => "\b"
        case "\\t" => "\t"
        case "\\r" => "\r"
        case "\\\"" => "\""
        case _ => m.group(0)
      })
  }

  def NUMBER: Parser[String] = """[0-9]+""".r ~ opt("""[.][0-9]+""") ^^ {
    case left ~ None => left
    case left ~ Some(right) => left + right
  }

  def KEY = DBSTRING
  def VALUE = STRING | NUMBER

  def PAIR: Parser[(String, String)] = KEY ~ (opt(whiteSpace) ~> "=" <~ opt(whiteSpace)) ~ VALUE ^^ {
    case left ~ _ ~ right => (left, right)
  }

  def OBJECT: Parser[QueryObject] = ("{" <~ opt(whiteSpace)) ~ repsep(PAIR, opt(whiteSpace) ~ "," ~ opt(whiteSpace)) ~ (opt(whiteSpace) ~> "}") ^^ {
    case _ ~ (middle: List[(String, String)]) ~ _ =>
      new QueryObject(middle)
  }

  // INSERT INTO tablestring VALUES { tags={...}, type=typestring, value=<typestring>::value }, ...

  def INSERT_INTO: Parser[InsertStatement] = (KW_INSERT <~ whiteSpace) ~ (DBSTRING <~ whiteSpace) ~ (KW_VALUES <~ whiteSpace) ~ rep1sep(OBJECT, opt(whiteSpace) ~ "," ~ opt(whiteSpace)) ^^ {
    case insert ~ table ~ values ~ objects =>
      new InsertStatement(table, objects)
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

  def STATEMENT: Parser[Statement] = opt(whiteSpace) ~> (JOIN_STMT | COUNT_FROM | INSERT_INTO) <~ opt(NEWLINE)
  def QUERY: Parser[StatementCollection] = rep1sep(STATEMENT, opt(whiteSpace) ~ SEMICOLON) ^^ { case s => new StatementCollection(s) }
}