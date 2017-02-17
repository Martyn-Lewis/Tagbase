package parser

class AndExpression(left: Expression, right: Expression) extends Expression {
  override def toString: String = "(" + left.toString + " & " + right.toString + ")"

  override def compile_method(): (Taggable) => Boolean = {
    val left_compile = left.compile_method()
    val right_compile = right.compile_method()

    (t) => if(left_compile(t)) right_compile(t) else true
  }

  override def generate_weight(index: Map[String, Double]): Double = (left.generate_weight(index) + right.generate_weight(index)) / 2

  override def contextual_optimise(index: Map[String, Double]): Expression = {
    // TODO
    val l = left.contextual_optimise(index)
    val r = right.contextual_optimise(index)

    if(l.generate_weight(index) < r.generate_weight(index)) new AndExpression(l, r)
    else new AndExpression(r, l)
  }

  override def context_free_optimise(): Expression = left.context_free_optimise() match {
    case t: NeverExpression => t
    case t: AlwaysExpression => right.context_free_optimise()
    case t => right.context_free_optimise() match {
      case c: NeverExpression => t
      case c: AlwaysExpression => t
      case c => new AndExpression(t, c)
    }
  }
}
