package parser

class QueryObject(val objects: List[(String, String)]) {
  override def toString: String = {
    val middle = objects map { case (l:String, r:String) => l + " = " + r  } mkString ", "
    s"Object($middle)"
  }
}
