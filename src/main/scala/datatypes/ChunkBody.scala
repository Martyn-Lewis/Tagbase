package datatypes

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

  override def next(): Option[ChunkBase] = front
  override def prev(): Option[ChunkBase] = Some(behind)
}
