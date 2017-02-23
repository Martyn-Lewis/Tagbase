package datatypes

abstract class ChunkBase {
  def next(): Option[ChunkBase]
  def prev(): Option[ChunkBase]
  def insert(element: DatabaseRow): Unit
  def sync_insert(element: DatabaseRow): Unit
  def calculate_size(): Int
}

