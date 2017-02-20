package parser

import datatypes.Taggable

class AndExpression(val left: Expression, val right: Expression) extends Expression {
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

    def cascade = {
      val optimised: Expression =
        if (l.generate_weight(index) < r.generate_weight(index)) new AndExpression(l, r).context_free_optimise()
        else new AndExpression(r, l).context_free_optimise()

      optimised match {
        case child: AndExpression =>
          if ((child.left != left && child.left != right) && (child.right != left && child.right != right))
            optimised.contextual_optimise (index)
          else
            optimised
        case _ => optimised.contextual_optimise(index)
      }
    }

    l match {
      case t: TagExpression => r match {
        case right: AllExpression => new AllExpression(right.children + t).contextual_optimise(index)
        case _ => cascade
      }
      case _ => cascade
    }
  }

  override def context_free_optimise(): Expression = {
    def right_check(t: Expression) = right.context_free_optimise() match {
      case c: NeverExpression => t
      case c: AlwaysExpression => t
      case c => new AndExpression(t, c)
    }
    left.context_free_optimise() match {
      case t: NeverExpression => t
      case t: AlwaysExpression => right.context_free_optimise()
      case t: AllExpression => right.context_free_optimise() match {
        case r: TagExpression => new AllExpression(t.children + r)
        case r: AllExpression => new AllExpression(r.children ++ t.children)
        case r => right_check(t)
      }
      case t: TagExpression => right.context_free_optimise() match {
        case r: TagExpression => new AllExpression(Set(t, r))
        case r: AllExpression => new AllExpression(r.children + t)
        case r => right_check(t)
      }
      case t => right_check(t)
    }
  }
}
