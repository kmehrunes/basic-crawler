package crawler

class DomainMatcher(seed: String) {
  private val corrected = URLs.correctUrl(seed)
  private val root: Option[String] = corrected.map(URLs.makeRoot)
  private val normalizedDomain: Option[String] = corrected.flatMap(URLs.domain)

  /*
   * There's no point in continuing if the seed isn't valid
   */
  if (corrected.isEmpty)
    throw new IllegalArgumentException("Couldn't parse " + seed + " as a valid URL")

  /**
    * Checks if a given URL belongs is within
    * the domain. All relative URLs are within
    * the domain by default.
    * @param url
    * @return
    */
  def sameDomain(url: String): Boolean = {
    URLs.protocol(url) match {
      case Some(protocol) =>
        if (protocol == "http" || protocol == "https")
          URLs.domain(url) == normalizedDomain
        else
          false

      case _ => !URLs.isNotSupported(url)
    }
  }

  /**
    * Converts a relative path to its absolute path
    * whether it' relative to the root of the website
    * or relative to the directory of the current page.
    * The latter isn't used often but still needs to
    * account for it.
    * @param url
    * @param current
    * @return
    */
  def fullPath(url: String, current: String = ""): Option[String] = {
    if (URLs.hasProtocol(url)) {
      Some(URLs.normalizeUrl(url))
    }
    else {
      url.charAt(0) match {
        case '/' => Some(URLs.normalizeUrl(root.get + url))
        case _ =>  URLs.directoryRelativeToAbsolute(URLs.normalizeUrl(url), current)
      }
    }
  }
}
