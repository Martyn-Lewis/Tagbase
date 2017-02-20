package parser

import datatypes.Taggable

class NegativeExpression(_expression: Expression) extends Expression {
  val expression: Expression = _expression
  override def toString: String = "-" + expression.toString

  override def context_free_optimise(): Expression = expression.context_free_optimise() match {
    case e: NeverExpression => new AlwaysExpression
    case e: NegativeExpression => e.expression.context_free_optimise()
    case e => new NegativeExpression(e)
  }

  override def contextual_optimise(index: Map[String, Double]): Expression = // TODO
    expression.contextual_optimise(index) match {
      case e: NeverExpression => new AlwaysExpression
      case e => new NegativeExpression(e)
    }

  override def compile_method(): (Taggable) => Boolean = expression match {
    case e: NegativeExpression => e.expression.compile_method() /* -(-(x)) == x */
    case e => (!(_: Boolean)) compose e.compile_method()
  }
}
