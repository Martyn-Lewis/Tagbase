package parser

class TagExpression(val tag: Tag) extends Expression {
  override def toString: String = tag.toString

  override def compile_method(): (Taggable) => Boolean = _.tags.contains(tag.word)
  override def context_free_optimise(): Expression = this
  override def contextual_optimise(index: Map[String, Double]): Expression =
    if(!index.contains(tag.word)) new NeverExpression
    else if(index(tag.word) == 1.0) new AlwaysExpression
    else this
  override def generate_weight(index: Map[String, Double]): Double =
    if(index.contains(tag.word)) index(tag.word)
    else 0
}
