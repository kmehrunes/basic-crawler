package crawler

trait SiteTraversal {
  def hasMore: Boolean
  def nextPage(): Option[String]
  def addPage(url: String, robots: Robots): Unit
  def addPages(urls: List[String], robots: Robots): Unit
}

trait VisitedPagesLog {
  def isVisited(url: String): Boolean
  def markVisited(url: String): Unit
}

trait TraversalQueue {
  def hasNext: Boolean
  def enqueue(url: String): Unit
  def dequeue(): Option[String]
}

case class Page(url: String, internalLinks: List[String], assets: List[String], code: Int = 200, error: String = "")
