package database_test

import parser.Taggable

class Database {
  var elements: List[Taggable] = List.empty
  var indexes: collection.mutable.Map[String, Int] = collection.mutable.Map[String, Int]()

  def add_element(element: Taggable): Unit = {
    elements ::= element
    element.tags foreach ((x) => {
      if(indexes.contains(x)) indexes(x) += 1
      else indexes(x) = 1
    })
  }

  def generate_weights(): Map[String, Double] = indexes mapValues ((v) => v.asInstanceOf[Double] / elements.size) toMap
}
