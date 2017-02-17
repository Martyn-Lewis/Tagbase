package parser

class ExpressionParser extends BasicParser {
  override val skipWhitespace = true

  def TAGEXPRESSION: Parser[TagExpression] = TAG ^^ { new TagExpression(_) }
  def FACTOR: Parser[Expression] = TAGEXPRESSION | ("(" ~> EXPRESSION <~ ")")

  def AND_EXPRESSION: Parser[Expression] = FACTOR ~ opt(opt(AMPERSAND) ~> AND_EXPRESSION) ^^ {
    case p ~ Some(right) => new AndExpression(p, right)
    case p ~ None => p
  }

  def OR_EXPRESSION: Parser[Expression] = AND_EXPRESSION ~ opt(PIPE ~ OR_EXPRESSION) ^^ {
    case p ~ Some(_ ~ right) => new OrExpression(p, right)
    case p ~ None => p
  }

  def TERM: Parser[Expression] = OR_EXPRESSION
  def NEGATIVE: Parser[Expression] = rep(MINUS) ~ TERM ^^ {
    case (l: List[String]) ~ p =>
      if(l.size % 2 == 0) p
      else new NegativeExpression(p)
  }
  def TERNARY_EXPRESSION: Parser[Expression] = NEGATIVE ~ opt(QUESTION ~ NEGATIVE ~ COLON ~ NEGATIVE) ^^ {
    case (p: Expression) ~ Some(_ ~ left ~ _ ~ right) => new TernaryExpression(p, left, right)
    case (p: Expression) ~ None => p
  }
  def EXPR: Parser[Expression] = TERNARY_EXPRESSION

  def EXPRESSION: Parser[Expression] = EXPR
}