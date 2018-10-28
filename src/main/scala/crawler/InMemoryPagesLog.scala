package crawler

import scala.collection.mutable

class InMemoryPagesLog extends VisitedPagesLog {
  private val visited = new mutable.HashSet[String]()

  override def isVisited(url: String): Boolean = visited.contains(url)

  override def markVisited(url: String): Unit = visited.add(url)
}
