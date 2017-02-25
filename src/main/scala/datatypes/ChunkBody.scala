package datatypes

import sun.font.TrueTypeFont

class ChunkBody(val size: Int, var behind: ChunkBase) extends ChunkBase {
  var front: Option[ChunkBody] = None
  val elements: Array[DatabaseRow] = new Array[DatabaseRow](size)
  var distance: Int = 0

  def ensure_next(default_size: Int): ChunkBody = next() match {
    case Some(body) => body.asInstanceOf[ChunkBody]
    case None =>
      val ahead = new ChunkBody(default_size, this)
      front = Some(ahead)
      ahead
  }

  override def calculate_size(): Int = front match {
    case Some(chunk) => distance + chunk.calculate_size()
    case None => distance
  }

  override def sync_insert(element: DatabaseRow) = insert(element)

  def insert(element: DatabaseRow): Unit =
    if(distance >= size) ensure_next(size).insert(element)
    else {
      elements(distance) = element
      distance += 1
    }

  def create_copy(): ChunkBody = {
    this.synchronized {
      val result = new ChunkBody(size, behind)
      result.front = front
      elements.copyToArray(result.elements, 0, size)
      result.distance = distance
      result
    }
  }

  override def delete_with(f: (DatabaseRow) => Boolean) = {
    val wiped_elements = elements.filter({
      case null => true
      case row: DatabaseRow => !f(row)
    })

    if(wiped_elements.length != elements.length) {
      val result = new ChunkBody(wiped_elements.length, behind)
      wiped_elements.copyToArray(result.elements)
      result.distance = size - wiped_elements.size
      result.front = front
      result.behind = behind
      Some(result)
    } else
      None
  }

  override def next(): Option[ChunkBase] = front
  override def prev(): Option[ChunkBase] = Some(behind)
}
