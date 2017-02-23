package datatypes

import scala.annotation.tailrec

class ChunkHead(var default_depth: Int) extends ChunkBase {
  var front: Option[ChunkBody] = None
  var tail: Option[ChunkBody] = None // Invalidate this when the chunk it points to is invalidated or bad things happen

  override def next(): Option[ChunkBase] = front
  override def prev(): Option[ChunkBase] = None

  override def calculate_size(): Int = front match {
    case Some(chunk) => chunk.calculate_size()
    case None => 0
  }

  override def insert(element: DatabaseRow): Unit = {
    @tailrec
    def lookahead(into: ChunkBody): ChunkBody = into.next() match {
      case Some(c: ChunkBody) => lookahead(c)
      case None => into
    }
    val chunk = tail match {
      case Some(chunk) =>
        if(chunk.distance == chunk.size)
          tail = Some(lookahead(chunk))
        chunk
      case None => front match {
        case Some(chunk) =>
          tail = Some(lookahead(chunk))
          chunk
        case None =>
          val ahead = new ChunkBody(default_depth, this)
          front = Some(ahead)
          ahead
      }
    }
    chunk.synchronized {
      chunk.insert(element)
    }
  }
}
