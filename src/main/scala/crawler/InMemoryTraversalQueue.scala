package crawler

import scala.collection.mutable

class InMemoryTraversalQueue extends TraversalQueue {
  private val queue = new mutable.Queue[String]()

  override def hasNext: Boolean = queue.nonEmpty

  override def enqueue(url: String): Unit = queue.enqueue(url)

  override def dequeue(): Option[String] = if (hasNext) Some(queue.dequeue()) else None
}
