package crawler

import org.jsoup._
import org.jsoup.nodes._
import org.jsoup.select.Elements

import scala.collection.mutable

object PageParser {
  val STYLE_CSS_SELECTOR = "link[rel=stylesheet]"
  val JS_CSS_SELECTOR = "script[src]"
  val LINK_CSS_SELECTOR = "a[href]"
  val IMG_CSS_SELECTOR = "img"
  val AUDIO_CSS_SELECTOR = "audio > source"
  val VIDEO_CSS_SELECTOR = "video > source"

  val HEADER_CSS_QUERY: String = List(STYLE_CSS_SELECTOR, JS_CSS_SELECTOR).mkString(",")
  val BODY_CSS_QUERY: String = List(STYLE_CSS_SELECTOR, JS_CSS_SELECTOR,
                                    LINK_CSS_SELECTOR, IMG_CSS_SELECTOR,
                                    AUDIO_CSS_SELECTOR, VIDEO_CSS_SELECTOR).mkString(",")

  def parseHtml(html: String): Option[Document] = {
    try {
      Some(Jsoup.parse(html))
    }
    catch {
      case _: Exception => None
    }
  }

  def selectElements(root: Element, selector: String): Elements = root.select(selector)

  def href(element: Element): String = element.attr("href")
  def src(element: Element): String = element.attr("src")

  def isInternalLink(link: String, matcher: DomainMatcher): Boolean = matcher.sameDomain(link)

  def classifyElements(elements: Elements, matcher: DomainMatcher): (List[String], List[String]) = {
    val links = new mutable.ListBuffer[String]()
    val assets = new mutable.ListBuffer[String]()
    val iter = elements.iterator()

    while (iter.hasNext) {
      val element = iter.next()
      element.tagName() match {
        case "a" =>
          val link = href(element)
          if (matcher.sameDomain(link))
            links += link

        case "link" => assets += href(element)

        case _ => assets += src(element) // images also have srcset but that requires special parsing so we ignored it
      }
    }

    (links.toList, assets.toList)
  }

  def parseDocument(url: String, document: Document, matcher: DomainMatcher): Page = {
    val head = document.head() // the head can contain scripts and styles (using <link>)
    val body = document.body() // for images, audio..etc and can contain scripts and styles too

    val headElements = selectElements(head, HEADER_CSS_QUERY)
    val bodyElements = selectElements(body, BODY_CSS_QUERY)

    val headClassified = classifyElements(headElements, matcher)
    val bodyClassified = classifyElements(bodyElements, matcher)

    Page(url, bodyClassified._1 ::: headClassified._1, bodyClassified._2 ::: headClassified._2)
  }
}
