package parser

import datatypes.Taggable

import scala.annotation.tailrec

class AnyExpression(var children: Set[TagExpression]) extends Expression {
  override def toString: String = "(" + (children map (_.toString)).mkString(" | ") + ")"

  override def generate_weight(index: Map[String, Double]): Double = {
    @tailrec
    def depreciating_sum(of: List[TagExpression], total: Double, count: Int): Double =
      if(of.isEmpty) total
      else depreciating_sum(of.tail, total + (of.head.generate_weight(index) / count), count + 1)
    depreciating_sum(children.toList, 0, 1)
  }

  override def compile_method(): (Taggable) => Boolean = (argument) => {
    children exists ((x) => argument.tags.contains(x.tag.word))
  }

  override def contextual_optimise(index: Map[String, Double]): Expression = {
    val valid = (children map (_.tag.word)) intersect index.keySet

    if(children.exists(_.generate_weight(index) == 1.00)) new AlwaysExpression
    else if(valid.nonEmpty) new AnyExpression(children filter ((t) => valid.contains(t.tag.word)))
    else new NeverExpression
  }
}
