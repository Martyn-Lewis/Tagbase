package parser

import datatypes.Taggable

class PointlesssExecutableExpression() extends ExecutableExpression {
  override def evaluate: Taggable => Boolean = (x) => false

  override def evaluate_many(t: Iterator[Taggable]): Iterator[Taggable] = Iterator.empty
}
