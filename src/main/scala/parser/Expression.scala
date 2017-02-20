package parser

import datatypes.Taggable

abstract class Expression() {
  def compile(): ExecutableExpression = compile_method() match {
    case _: NeverExpression => new PointlesssExecutableExpression
    case _: AlwaysExpression => new EagerExecutableExpression
    case _ => new ActuallyExecutableExpression(compile_method())
  }
  def compile_method(): Taggable => Boolean
  def context_free_optimise(): Expression = this
  def contextual_optimise(index: Map[String, Double]): Expression = this
  def full_optimise(index: Map[String, Double]): Expression = contextual_optimise(index).context_free_optimise()
  def generate_weight(index: Map[String, Double]): Double = 0.5
}
