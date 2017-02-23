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

  override def sync_insert(element: DatabaseRow) = {
    // Copy paste until I properly refactor this
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
    chunk.insert(element)
  }

  override def insert(element: DatabaseRow): Unit = {
    // An overzealous assumption is made here that the parent pool will be synchronised.
    // If that is not the case, then the likes of tail/etc may simply be invalid.
    // I'm not sure if I should remove this assumption since it may cost even more performance.
    // I likely should.

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
