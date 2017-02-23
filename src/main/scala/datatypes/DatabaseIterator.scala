package datatypes

import scala.collection.AbstractIterator

class DatabaseIterator(database: Database) extends AbstractIterator[DatabaseRow] {
  var current_chunk: Option[ChunkBody] = database.head_chunk.next().asInstanceOf[Option[ChunkBody]]
  var offset: Int = 0

  current_chunk match {
    case Some(p) => if(p.distance == 0) current_chunk = None // Shave important time from next
  }

  override def hasNext: Boolean = current_chunk.isDefined
  override def next(): DatabaseRow =
    current_chunk match {
      case Some(chunk) =>
        val result = chunk.elements(offset)
        offset += 1
        if(offset >= chunk.size || offset >= chunk.distance) {
          current_chunk = chunk.next() match {
            case None => None
            case Some(p: ChunkBody) =>
              if(p.distance > 0) {
                offset = 0
                if (p.distance < p.size)
                  p.synchronized {
                    // For thread-safety we make a new reference to p's elements that doesn't
                    // have its distance value updated. Saves the need for copying.
                    Some(new ChunkDeferred(p.size, p.behind, p.front, p.distance, p.elements))
                  }
                else Some(p)
              }
              else None
          }
        }
        result
      case None => throw new RuntimeException("next when hasNext is false")
    }
}
