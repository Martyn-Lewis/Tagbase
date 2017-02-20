package datatypes

class DatabaseRow(val contents: String, tags_to_use: Set[String]) extends Taggable {
  override var tags: Set[String] = tags_to_use
}
