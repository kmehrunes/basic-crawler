package test

import org.scalatest.FunSuite
import crawler.{DomainMatcher, URLs}

class DomainMatcherTests extends FunSuite {
  test("Correct URL-Matching") {
    // URLs that should match
    assert(URLs.correctUrl("http://www.test.org").isDefined)
    assert(URLs.correctUrl("http://test.org").isDefined)
    assert(URLs.correctUrl("https://test.org").isDefined)
    assert(URLs.correctUrl("test.org").isDefined)
    assert(URLs.correctUrl("forums.test.org").isDefined)
    assert(URLs.correctUrl("test.org/").isDefined)

    // URLs that should fail
    assert(URLs.correctUrl("ftp://test.org").isEmpty)
    assert(URLs.correctUrl("://test.org").isEmpty)
    assert(URLs.correctUrl("/test.org").isEmpty)
    assert(URLs.correctUrl("mailto:email@test.org").isEmpty)
    assert(URLs.correctUrl("test").isEmpty)
    assert(URLs.correctUrl(".test").isEmpty)
    assert(URLs.correctUrl(".test.org").isEmpty)
    assert(URLs.correctUrl("test.").isEmpty)
    assert(URLs.correctUrl("www.test.").isEmpty)
  }

  test("Correct URL-Values") {
    assert(URLs.correctUrl("http://www.test.org").get == "http://www.test.org")
    assert(URLs.correctUrl("test.org").get == "http://test.org")
    assert(URLs.correctUrl("forums.test.org").get == "http://forums.test.org")
    assert(URLs.correctUrl("forums.test.org/").get == "http://forums.test.org/")
  }

  test("Normalize host") {
    assert(URLs.normalizeHost("www.test.org") == "test.org")
    assert(URLs.normalizeHost("forums.test.org") == "test.org")
    assert(URLs.normalizeHost("test.org") == "test.org")
  }

  test("Domain") {
    assert(URLs.domain("http://www.test.org").isDefined)
    assert(URLs.domain("http://www.test.org").getOrElse("") == "test.org")

    assert(URLs.domain("http://www.test.org/").isDefined)
    assert(URLs.domain("http://www.test.org/").getOrElse("") == "test.org")

    assert(URLs.domain("http://test.org/").isDefined)
    assert(URLs.domain("http://test.org/").getOrElse("")  == "test.org")

    assert(URLs.domain("http://test.org").isDefined)
    assert(URLs.domain("http://test.org").getOrElse("")  == "test.org")

    assert(URLs.domain("http://www.test.org/index.html").isDefined)
    assert(URLs.domain("http://www.test.org/index.html").getOrElse("")  == "test.org")

    assert(URLs.domain("http://www.test.org/index.html?i=0").isDefined)
    assert(URLs.domain("http://www.test.org/index.html?i=0").getOrElse("")  == "test.org")
  }

  test("Make root") {
    assert(URLs.makeRoot("http://www.test.org") == "http://www.test.org")
    assert(URLs.makeRoot("http://www.test.org/") == "http://www.test.org")
  }

  test("Domain matching") {
    val domainMatcher = new DomainMatcher("http://www.test.org")

    // should match
    assert(domainMatcher.sameDomain("http://www.test.org/mock.html"))
    assert(domainMatcher.sameDomain("/index.html"))
    assert(domainMatcher.sameDomain("index.html"))
    assert(domainMatcher.sameDomain("/index.html"))
    assert(domainMatcher.sameDomain("/index/one"))
    assert(domainMatcher.sameDomain("www.test2.org")) // yes it should match! both Firefox and Chromium considered it a relative URL

    // shouldn't match
    assert(!domainMatcher.sameDomain("http://www.test2.org"))
    assert(!domainMatcher.sameDomain("https://www.test2.org"))
    assert(!domainMatcher.sameDomain("ftp://www.test2.org")) // we don't care about other protocol
    assert(!domainMatcher.sameDomain("http://www.test2.org/"))
    assert(!domainMatcher.sameDomain("mailto:admin@test.org"))

    // full path
    assert(domainMatcher.fullPath("/index.html").isDefined)
    assert(domainMatcher.fullPath("/index.html").get == "http://www.test.org/index.html")

    assert(domainMatcher.fullPath("index.html").isEmpty)
    assert(domainMatcher.fullPath("index.html", "http://www.test.org/current.html").isDefined)
    assert(domainMatcher.fullPath("index.html", "http://www.test.org/current.html").get == "http://www.test.org/index.html")

    assert(domainMatcher.fullPath("http://www.test.org").isDefined)
    assert(domainMatcher.fullPath("http://www.test.org").get == "http://www.test.org")
  }
}
