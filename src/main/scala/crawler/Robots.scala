package crawler

import java.net.HttpURLConnection
import java.util.concurrent.TimeoutException

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import scalaj.http.{Http, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global

class Robots(val domainMatcher: DomainMatcher) {
  private val ROBOTS_TXT_FILE = "/robots.txt"
  private val timeout = Duration.create("2s")

  private def retrieveFile(): Either[Throwable, HttpResponse[String]] = {
    val f = Future {
      domainMatcher.fullPath(ROBOTS_TXT_FILE) match {
        case Some(url) => Http(url).asString
        case None => Failure(new IllegalArgumentException())
      }
    }

    try {
      val result = Await.ready(f, timeout).value.getOrElse(Failure(new TimeoutException))
      result match {
        case Success(response: HttpResponse[String]) => Right(response)
        case Failure(ex) => Left(ex)
      }
    }
    catch {
      case ex: Exception => Left(ex)
    }
  }

  private def parseRobots(response: String): List[String] = List()

  def retrieve(): Unit = retrieveFile() match {
      case Right(response: HttpResponse[String]) =>
        if (response.code == HttpURLConnection.HTTP_OK)
          parseRobots(response.body)
      case Left(_: Throwable) => ()
    }

  def canCrawl(url: String): Boolean = true
}
