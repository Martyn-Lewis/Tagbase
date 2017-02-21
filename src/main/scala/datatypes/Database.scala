package datatypes

class Database(val enabled_indexes: Boolean) {
  var elements: List[Taggable] = List.empty
  var indexes: collection.mutable.Map[String, Int] = collection.mutable.Map[String, Int]()
  var index_records: collection.mutable.Map[String, List[Taggable]] = collection.mutable.Map[String, List[Taggable]]()

  def add_element(element: Taggable): Unit = {
    elements ::= element
    element.tags foreach ((x) => {
      if(indexes.contains(x)) indexes(x) += 1
      else indexes(x) = 1

      if(enabled_indexes) {
        if (!index_records.contains(x))
          index_records(x) = List.empty
        index_records(x) ::= element
      }
    })
  }

  def generate_weights(): Map[String, Double] = indexes.mapValues((v) => v.asInstanceOf[Double] / elements.size).toMap
}
