package crawler

import java.io.PrintStream

import scala.collection.mutable

object StandardSiteMap {
  private def writePage(page: Page)(writer: PrintStream): Unit = page.code match {
    case 200 =>
      val listedAssets = new mutable.HashSet[String]()
      val listedLinks = new mutable.HashSet[String]()

      writer.println(page.url)
      writer.println("|   |_assets:")
      page.assets.foreach(asset =>
        if (!listedAssets.contains(asset)) {
          writer.println("|     |_ " + asset)
          listedAssets += asset
        }
      )

      writer.println("|_ links:")
      page.internalLinks.foreach(link =>
        if (!listedLinks.contains(link)) {
          writer.println("  |_ " + link)
          listedLinks += link
        }
      )

    case _ => writer.println(page.url + "  Code: " + page.code + " " + page.error)
  }

  def write(pages:List[Page], writer: PrintStream): Unit = {
    pages.foreach(writePage(_)(writer))
  }
}
