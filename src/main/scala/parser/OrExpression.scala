package parser

import datatypes.Taggable

class OrExpression(left: Expression, right: Expression) extends Expression {
  override def toString: String = "(" + left.toString + " | " + right.toString + ")"

  override def generate_weight(index: Map[String, Double]): Double = left.generate_weight(index) + (right.generate_weight(index) / 2)

  override def context_free_optimise(): Expression = left.context_free_optimise() match {
    case _: NeverExpression => right.context_free_optimise()
    case t: AlwaysExpression => t
    case t: TagExpression => right.context_free_optimise() match {
      case child: TagExpression => new AnyExpression(Set(t, child))
      case child: AnyExpression => child.children += t
        child
      case child: AlwaysExpression => child
      case _ => this
    }
    case a: AnyExpression => right.context_free_optimise() match {
      case child: TagExpression => a.children += child
        a
      case child: AnyExpression => a.children |= child.children
        a
      case child: AlwaysExpression => child
      case _: NeverExpression => a
      case child => new OrExpression(a, child)
    }
    case t => right.context_free_optimise() match {
      case _: NeverExpression => t
      case a: AlwaysExpression => a
      case a => new OrExpression(t, a)
    }
  }

  override def contextual_optimise(index: Map[String, Double]): Expression = {
    (left.contextual_optimise(index) match {
      case t: TagExpression =>
        if(index.keySet.contains(t.tag.word)) right.contextual_optimise(index) match {
          case child: TagExpression =>
            if(index.keySet.contains(t.tag.word))
            {
              if(t.generate_weight(index) > child.generate_weight(index)) new OrExpression(t, child)
              else new OrExpression(child, t)
            }
            else t
          case child: NeverExpression => t
          case child =>
            if(t.generate_weight(index) > child.generate_weight(index)) new OrExpression(t, child)
            else new OrExpression(child, t)
        }
        else right.contextual_optimise(index)
      case t: NeverExpression => right.contextual_optimise(index)
      case t => {
        val r = right.contextual_optimise(index)
        if(t.generate_weight(index) > r.generate_weight(index)) new OrExpression(t, r)
        else new OrExpression(r, t)
      }
    }).context_free_optimise()
  }

  override def compile_method(): (Taggable) => Boolean = {
    val left_compile = left.compile_method()
    val right_compile = right.compile_method()

    (t) => if(left_compile(t)) true else right_compile(t)
  }
}
