package parser

import datatypes.Taggable

class AllExpression(var children: Set[TagExpression]) extends Expression {
  override def toString: String = "(" + (children map (_.toString)).mkString(" & ") + ")"

  override def generate_weight(index: Map[String, Double]): Double = children.foldLeft(0.0)(_ + _.generate_weight(index))

  override def compile_method(): (Taggable) => Boolean = {
    val childTags = (children map (_.toString)).toSet
    childTags subsetOf _.tags
  }

  override def contextual_optimise(index: Map[String, Double]): Expression = {
    val relevant = (children filter (_.generate_weight(index) != 1.0))
    val valid = (relevant map (_.tag.word)) intersect index.keySet

    if(valid.size != relevant.size) new NeverExpression
    else if (relevant.size == 1) relevant.toIterator.next()
    else new AllExpression(relevant)
  }
}
