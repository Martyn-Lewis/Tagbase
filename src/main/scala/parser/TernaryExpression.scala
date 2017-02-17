package parser

class TernaryExpression(predicate: Expression, left: Expression, right: Expression) extends Expression {
  override def toString: String = predicate.toString + " ? " + left.toString + " : " + right.toString

  override def compile_method(): (Taggable) => Boolean = {
    val left_compile = left.compile_method()
    val right_compile = right.compile_method()
    val predicate_compile = predicate.compile_method()

    (t) => if(predicate_compile(t)) left_compile(t) else right_compile(t)
  }

  override def contextual_optimise(index: Map[String, Double]): Expression = // TODO
    new TernaryExpression(predicate.contextual_optimise(index), left.contextual_optimise(index), right.contextual_optimise(index))

  override def context_free_optimise(): Expression = predicate.context_free_optimise() match {
    case t: NeverExpression => right.context_free_optimise() /* never ? x : y = y */
    case t: AlwaysExpression => left.context_free_optimise() /* always ? x : y = x */
    case t => left.context_free_optimise() match {
      case l: NeverExpression => right.context_free_optimise() match {
        case r: NeverExpression => new NeverExpression /* x ? never : never = never */
        case r: AlwaysExpression => new NegativeExpression(t).context_free_optimise() /* x ? false : true == !x */
        case r => new AndExpression(new NegativeExpression(t).context_free_optimise(), r).context_free_optimise() /* x ? never : y is equal to -x & y */
      }
      case l: AlwaysExpression => right.context_free_optimise() match {
        case r: NeverExpression => t /* x ? true : false = x */
        case r: AlwaysExpression => new AlwaysExpression /* x ? true : true = true */
        case r => new TernaryExpression(t, l, r)
      }
      case l => right.context_free_optimise() match {
        case r: NeverExpression => /* x ? y : never is equal to x & y */
          new AndExpression(t, l).context_free_optimise()
        case r => new TernaryExpression(t, l, r)
      }
    }
  }
}
