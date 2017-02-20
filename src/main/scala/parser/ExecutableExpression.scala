package parser

import datatypes.Taggable

abstract class ExecutableExpression {
  def evaluate: Taggable => Boolean
  def evaluate_many(t: Iterator[Taggable]): Iterator[Taggable]
}
