package parser

class EagerExecutableExpression() extends ExecutableExpression {
  override def evaluate: Taggable => Boolean = (x) => true

  override def evaluate_many(t: Iterator[Taggable]): Iterator[Taggable] = t
}
