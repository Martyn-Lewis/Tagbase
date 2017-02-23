package datatypes

import scala.collection.mutable.ArrayBuffer

class Database(val enabled_indexes: Boolean) {
  val head_chunk = new ChunkHead(Database.default_chunk_depth)

  var indexes: collection.mutable.Map[String, Int] = collection.mutable.Map[String, Int]()
  var index_records: collection.mutable.Map[String, ArrayBuffer[Taggable]] = collection.mutable.Map[String, ArrayBuffer[Taggable]]()

  def sync_add_element(element: DatabaseRow): Unit = {
    head_chunk.sync_insert(element)
    element.tags foreach ((x) => {
      if (indexes.contains(x)) indexes(x) += 1
      else indexes(x) = 1

      if (enabled_indexes) {
        if (!index_records.contains(x))
          index_records(x) = ArrayBuffer.empty[Taggable]
        index_records(x) += element
      }
    })
  }

  def add_element(element: DatabaseRow): Unit = {
    this.synchronized {
      head_chunk.insert(element)
      element.tags foreach ((x) => {
        if (indexes.contains(x)) indexes(x) += 1
        else indexes(x) = 1

        if (enabled_indexes) {
          if (!index_records.contains(x))
            index_records(x) = ArrayBuffer.empty[Taggable]
          index_records(x) += element
        }
      })
    }
  }

  def generate_weights(): Map[String, Double] = indexes.mapValues((v) => v.asInstanceOf[Double] / head_chunk.calculate_size).toMap
}

object Database {
  val default_chunk_depth = 512
}
