import java.util.logging.{Level, Logger}

import crawler._

import scala.util.Try

object Main {

  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      System.err.println("Requires the seed URL to start from and the number of threads")
      System.exit(-1)
    }

    val FIREFOX_USERAGENT = "Mozilla/5.0" // some website will trigger 401 if a user-agent isn't set
    val numThreads = Try(args(1).toInt).toOption

    if (numThreads.isEmpty) {
      System.err.println("Failed to parse " + args(1) + " as a number")
      System.exit(-1)
    }

    if (numThreads.get < 1) {
      System.err.println("Number of threads should greater than 0")
      System.exit(-1)
    }

    val logger = Logger.getLogger("Crawler") // replace with a file logger if needed
    logger.setLevel(Level.ALL) // use Level.OFF to disable logging, it slows down the process

    val pagesLog = new InMemoryPagesLog
    val urlsQueue = new InMemoryTraversalQueue

    val crawler: Crawler = new Crawler(args(0), new BFSiteTraversal(urlsQueue, pagesLog), logger,
                                        redirect=true, FIREFOX_USERAGENT)
    crawler.start(numThreads.get - 1)
    StandardSiteMap.write(crawler.getPages, System.out)
  }

}
