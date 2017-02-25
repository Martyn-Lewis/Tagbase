package datatypes

import scala.annotation.tailrec

class ChunkHead(var default_depth: Int) extends ChunkBase {
  var front: Option[ChunkBody] = None
  var tail: Option[ChunkBody] = None // Invalidate this when the chunk it points to is invalidated or bad things happen

  override def next(): Option[ChunkBase] = front
  override def prev(): Option[ChunkBase] = None

  override def delete_with(f: (DatabaseRow) => Boolean) = {
    // This is a fairly odd method, so I will likely change it in the future.
    // It recursively goes through each chunk and calls its delete_with method with our f.
    //  If said delete_with returns None, then we skip it, as it's deleted nothing.
    //  If said delete_with returns Some(body), then we do an in place swap from the old chunk to the new chunk(s)
    //    It's important to note the plural here, as the ChunkBody we received may actually have its own front to new chunks.

    def link_forward(c: ChunkBody) = c.behind match {
        // Guarantee that some chunk c's predecessor behind points to c
      case behind: ChunkHead => behind.synchronized { behind.front = Some(c) }
      case behind: ChunkBody => behind.synchronized { behind.front = Some(c) }
    }

    @tailrec
    def lookahead(c: ChunkBody, target: ChunkBody): ChunkBody = c.front match {
      case Some(forward: ChunkBody) =>
        if(forward.equals(target))
          c
        else
          lookahead(forward, target)
      case None => throw new RuntimeException("Looking for a chunk but ended up with None");
    }

    @tailrec
    def recurse(chunk: Option[ChunkBody]): Unit = chunk match {
      case Some(c: ChunkBody) => {
        val ahead = c.front
        c.delete_with(f) match {
          case Some(diff: ChunkBody) =>
            if(diff.size == 0)
              recurse(ahead) // Delete empty chunks
            else {
              link_forward(diff)
              ahead match {
                case Some(forward: ChunkBody) =>
                  forward.synchronized {
                    forward.behind = lookahead(diff, forward)
                  }
                  recurse(Some(forward))
                case None => None
              }
            }
          case None => recurse(c.front)
        }
      }
      case None => None
    }

    this.synchronized {
      // I've not tested if this synchronise is actually necessary, but it does seem like unintended things would happen otherwise.
      recurse(front)
    }

    None
  }

  override def calculate_size(): Int = front match {
    case Some(chunk) => chunk.calculate_size()
    case None => 0
  }

  override def sync_insert(element: DatabaseRow) = {
    sync_get_latest_chunk().insert(element)
  }

  def sync_get_latest_chunk(): ChunkBody = {
    @tailrec
    def lookahead(into: ChunkBody): ChunkBody = into.next() match {
      case Some(c: ChunkBody) => lookahead(c)
      case None => into
    }

    tail match {
      case Some(chunk) =>
        if (chunk.distance == chunk.size)
          tail = Some(lookahead(chunk))
        chunk
      case None => front match {
        case Some(chunk) =>
          tail = Some(lookahead(chunk))
          chunk
        case None =>
          val ahead = new ChunkBody(default_depth, this)
          front = Some(ahead)
          tail = front
          ahead
      }
    }
  }

  def async_get_latest_chunk(): ChunkBody = {
    this.synchronized {
      sync_get_latest_chunk()
    }
  }

  override def insert(element: DatabaseRow): Unit = {
    val chunk = async_get_latest_chunk()

    chunk.synchronized {
      chunk.insert(element)
    }
  }
}
