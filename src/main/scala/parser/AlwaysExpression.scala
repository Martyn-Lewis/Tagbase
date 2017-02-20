package parser

import datatypes.Taggable

class AlwaysExpression() extends Expression {
  override def toString: String = "always"

  override def compile_method(): Taggable => Boolean = (x) => true
  override def context_free_optimise(): Expression = this
  override def contextual_optimise(index: Map[String, Double]): Expression = this

  override def generate_weight(index: Map[String, Double]): Double = 1
}
