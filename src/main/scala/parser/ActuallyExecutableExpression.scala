package parser

import datatypes.Taggable

class ActuallyExecutableExpression(method: Taggable => Boolean) extends ExecutableExpression {
  def evaluate: Taggable => Boolean = method
  def evaluate_many(t: Iterator[Taggable]): Iterator[Taggable] = t filter method
}
