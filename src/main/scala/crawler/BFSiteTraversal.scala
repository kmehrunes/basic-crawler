package crawler

class BFSiteTraversal(queue: TraversalQueue, pagesLog: VisitedPagesLog) extends SiteTraversal {
  override def hasMore: Boolean = this.synchronized {
    queue.hasNext
  }

  override def nextPage(): Option[String] = this.synchronized {
    queue.dequeue()
  }

  override def addPage(url: String, robots: Robots): Unit = this.synchronized {
    unsafeAddPage(url, robots)
  }

  override def addPages(urls: List[String], robots: Robots): Unit = this.synchronized {
    urls.foreach(url => unsafeAddPage(url, robots))
  }

  private def unsafeAddPage(url: String, robots: Robots): Unit = {
    if (!pagesLog.isVisited(url) && robots.canCrawl(url)) {
      queue.enqueue(url)
      pagesLog.markVisited(url)
    }
  }
}
