package test

import org.scalatest.FunSuite
import crawler.{DomainMatcher, PageParser}
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

class PageParserTests extends FunSuite {
  val html = "<!DOCTYPE html>\n" +
    "<html>" +
    "\n<head>\n" +
    // links to CSS files (valid and invalid)
    "<link rel=\"stylesheet\" type=\"text/css\" href=\"theme.css\">\n" + // valid
    "<link rel=\"notstyle\" type=\"text/css\" href=\"theme.css\">\n" + // invalid
    "<link rel=\"\" type=\"text/css\" href=\"theme.css\">\n" + // invalid
    "<link type=\"text/css\" href=\"theme.css\">\n" + // invalid
    // scripts sources
    "<script src=\"main.js\">" + // valid
    "<script> </script>" +  // invalid
    "</head>\n" +
    "<body>\n" +
    "<h1>My First Heading</h1>\n" +
    "<p>My first paragraph.</p>\n" +
    "<a href=\"www.some.com\"> link </a>\n" + // counts
    "<audio> <source src=\"audio.mp3\"> </audio>\n" + // counts
    "<video> <source src=\"video.mp4\"> </video>\n" + // counts
    "<img src=\"image.jpg\"/>\n" + // counts
    "<link rel=\"stylesheet\" type=\"text/css\" href=\"theme.css\">\n" + // counts
    "<script src=\"main.js\">" +  // counts
    "</body>\n" +
    "</html>"

  test("Parse HTML") {
    assert(PageParser.parseHtml(html).isDefined)
  }

  test("Head selectors") {
    val head = PageParser.parseHtml(html).get.head()
    val elements = PageParser.selectElements(head, PageParser.HEADER_CSS_QUERY)

    assert(elements.size() == 2) // only a single CSS and a single JS will match

    assert(elements.get(0).tagName() == "link")
    assert(PageParser.href(elements.get(0)) == "theme.css")

    assert(elements.get(1).tagName() == "script")
    assert(PageParser.src(elements.get(1)) == "main.js")
  }

  test("Body selectors") {
    val body = PageParser.parseHtml(html).get.body()
    val elements = PageParser.selectElements(body, PageParser.BODY_CSS_QUERY)

    assert(elements.size() == 6)

    assert(elements.get(0).tagName() == "a")
    assert(PageParser.href(elements.get(0)) == "www.some.com")

    assert(elements.get(1).tagName() == "source")
    assert(PageParser.src(elements.get(1)) == "audio.mp3")

    assert(elements.get(2).tagName() == "source")
    assert(PageParser.src(elements.get(2)) == "video.mp4")

    assert(elements.get(3).tagName() == "img")
    assert(PageParser.src(elements.get(3)) == "image.jpg")

    assert(elements.get(4).tagName() == "link")
    assert(PageParser.href(elements.get(4)) == "theme.css")

    assert(elements.get(5).tagName() == "script")
    assert(PageParser.src(elements.get(5)) == "main.js")
  }

  test("Classification") {
    val matcher = new DomainMatcher("http://www.test.org")
    val elements = new Elements()
    val style = new Element("link")
    val script = new Element("script")
    val inLinkRelative = new Element("a")
    val inLinkAbsolute = new Element("a")
    val outLink = new Element("a")
    val img = new Element("img")
    val audioSource = new Element("source")
    val videoSource = new Element("source")

    style.attr("href", "theme.css")
    elements.add(style)

    script.attr("src", "main.js")
    elements.add(script)

    inLinkRelative.attr("href", "/index/one")
    elements.add(inLinkRelative)

    inLinkAbsolute.attr("href", "http://www.test.org/index/one")
    elements.add(inLinkAbsolute)

    outLink.attr("href", "http://www.test2.org")
    elements.add(outLink)

    img.attr("src", "image.png")
    elements.add(img)

    audioSource.attr("src", "audio.mp3")
    elements.add(audioSource)

    videoSource.attr("src", "video.mp4")
    elements.add(videoSource)

    val (links, assets) = PageParser.classifyElements(elements, matcher)

    assert(links.size == 2)
    assert(links.contains("/index/one"))
    assert(links.contains("http://www.test.org/index/one"))

    assert(assets.size == 5)
    assert(assets.contains("theme.css"))
    assert(assets.contains("main.js"))
    assert(assets.contains("image.png"))
    assert(assets.contains("audio.mp3"))
    assert(assets.contains("video.mp4"))
  }
}
