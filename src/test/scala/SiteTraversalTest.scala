package test

import org.scalatest.FunSuite

import crawler._

class SiteTraversalTest extends FunSuite {
  test("Breadth-first traversal") {
    val traversal = new BFSiteTraversal(new InMemoryTraversalQueue, new InMemoryPagesLog)
    val robots = new Robots(new DomainMatcher("www.test.org"))
    traversal.addPage("www.test.org", robots)
    traversal.addPages(List("www.test.org/p1", "www.test.org/p2"), robots)
    traversal.addPage("www.test.org/p1", robots) // this one shouldn't be added since it's visited already

    assert(traversal.hasMore)

    assert(traversal.nextPage().get == "www.test.org")
    assert(traversal.hasMore)

    assert(traversal.nextPage().get == "www.test.org/p1")
    assert(traversal.hasMore)

    assert(traversal.nextPage().get == "www.test.org/p2")
    assert(!traversal.hasMore)
  }
}
