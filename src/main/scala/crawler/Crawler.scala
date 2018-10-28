package crawler

import java.net.HttpURLConnection
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{TimeUnit, TimeoutException}
import java.util.logging.Logger

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import scalaj.http._
import scala.concurrent.ExecutionContext.Implicits.global // futures need an execution context

class Crawler(val seed: String, val traversal: SiteTraversal, val logger: Logger,
              val redirect: Boolean = true, val userAgent: String = "") {
  private val domainMatcher = new DomainMatcher(seed)
  private val robots = new Robots(domainMatcher)
  private val pages = new mutable.ListBuffer[Page]()

  private val masterWaitPeriod = 1000 // in ms
  private val responseTimeout = Duration.create("2s")
  private val threadsWaitTimeout = 3 // in seconds

  private val correctedSeed = URLs.correctUrl(seed)

  /**
    * Issues an HTTP request, waits for the timeout
    * or a response.
    * @param url
    * @return Either a throwable error, or an HTTP response
    *         which could be an HTTP error response.
    */
  private def retrievePage(url: String): Either[Throwable, HttpResponse[String]] = {
    val f = Future {
      Http(url).header("User-Agent", userAgent)
                .option(HttpOptions.followRedirects(redirect))
                .asString
    }

    /*
     * Try-catch to avoid any exceptions thrown by the
     * java code behind scalaj.
     */
    try {
      val result = Await.ready(f, responseTimeout).value.getOrElse(Failure(new TimeoutException))
      result match {
        case Success(html) => Right(html)
        case Failure(ex: Throwable) => Left(ex)
      }
    }
    catch {
      case ex: Exception => Left(ex)
    }
  }

  private def processResponse(url: String, response: HttpResponse[String]): Page = {
    response.code match {
      case HttpURLConnection.HTTP_OK =>
        PageParser.parseHtml(response.body).map(doc => PageParser.parseDocument(url, doc, domainMatcher))
        .getOrElse(Page(url, Nil, Nil, 0, "failed to parse"))

      case _ => Page(url, Nil, Nil, response.code)
    }
  }

  private def addPage(page: Page): Unit = pages.synchronized {
    pages += page
    traversal.addPages(page.internalLinks, robots)
  }

  private def process(url: String): Page = {
    retrievePage(url) match {
      case Right(response: HttpResponse[String]) =>
        processResponse(url, response)

      case Left(ex: HttpStatusException) => Page(url, Nil, Nil, ex.code)

      case Left(_: TimeoutException) => Page(url, Nil, Nil, -1, "timeout")

      case Left(ex: Throwable) => Page(url, Nil, Nil, -1, ex.toString)
    }
  }

  /**
    * Starts the crawling process. The execution model goes
    * like this:
    * - A master thread (the current thread) does the following:
    *   -- get the next page in the queue
    *   -- if there was a page to be processed do the following:
    *     --- process the page
    *     --- create either the maximum number of slave threads or as many threads as
    *         in-domain links in the current page
    *   -- if there was nothing in the queue do the following:
    *     --- if there are some threads running, wait and check again since one of them
    *         might add a new page
    *
    * - A slave thread is similar to a master thread by doesn't
    *   create new threads, and doesn't wait. It, instead, notifies the master thread
    *   that it has existed so that if it needs a new thread it can create one
    *
    * @param numSlaveThreads The maximum number of slave threads to be created so
    *                        that at any point, there's only numThreads + 1 threads
    *                        running.
    */
  def start(numSlaveThreads: Int): Unit = {
    val pool = java.util.concurrent.Executors.newFixedThreadPool(numSlaveThreads)
    val remaining: AtomicInteger = new AtomicInteger(numSlaveThreads) // -1 since we already have a master thread

    robots.retrieve() // need to get the robots rules first
    traversal.addPage(correctedSeed.get, robots)

    def slaveTask(): Unit = traversal.nextPage() match {
      case Some(next) =>
        if (robots.canCrawl(next)) {
          val full = domainMatcher.fullPath(next)
          logger.info("Slave-Crawling " + next + "(" + full + ")")
          full.foreach(url => {
            val page = process(url)
            addPage(page)
          })
        }

        slaveTask()

      case None =>
        logger.info("Slave-Nothing to crawl")
        remaining.incrementAndGet()
    }

    def masterTask(): Unit = traversal.nextPage() match {
      case Some(next) =>
        val full = domainMatcher.fullPath(next)
        logger.info("Master-Crawling " + next + "(" + full + ")")

        full.foreach(url => {
          val page = process(url)
          addPage(page)

          val cache = remaining.get()
          for (_ <- 0 until Math.min(cache, page.internalLinks.size)) {
            pool.execute(() => slaveTask())
            remaining.decrementAndGet()
            logger.info("Master-Creating new thread. Remaining: " + remaining.get())
          }
        })

        masterTask()

      case None =>
        if (remaining.get() < numSlaveThreads) { // some threads are still running, wait maybe they'll add more pages
          Thread.sleep(masterWaitPeriod)
          masterTask()
        }
    }

    masterTask()

    pool.shutdown() // doesn't actually end current running threads, it just doesn't accept more
    pool.awaitTermination(threadsWaitTimeout, TimeUnit.SECONDS)
  }

  def getPages: List[Page] = pages.toList
}
