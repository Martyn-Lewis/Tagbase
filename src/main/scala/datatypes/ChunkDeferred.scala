package datatypes

class ChunkDeferred(override val size: Int, var _behind: ChunkBase,
                    var _front: Option[ChunkBody], var _distance: Int,
                    override val elements: Array[DatabaseRow]) extends ChunkBody(size, _behind) {
  front = _front
  distance = _distance
}
