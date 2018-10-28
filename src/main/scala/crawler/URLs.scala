package crawler

import java.net.URL
import java.util.regex.Pattern

/**
  * Created by kld on 28/10/18.
  */
object URLs {
  /**
    * This regex matches a URL with no protocol.
    * It accepts them in the form of "something.something[.something]".
    * However, it still matches "something.something."
    * which is why another regex is needed to match
    * end of the URL.
    */
  val noProtocolPattern: Pattern = Pattern.compile("^\\w+\\.\\w+(?:\\.\\w+)?")

  /**
    * A regex to complement the previous one. It makes sure that
    * the URL ends with ".something[/]".
    */
  val endingPattern: Pattern = Pattern.compile("\\.\\w+(?:\\/)?$")

  /**
    * A regex to check if a certain text is not supported
    * even though it can pass all other tests. Currently
    * only blocks "action:whatever" (e.g. "mailto:....").
    */
  val notSupportedPattern: Pattern = Pattern.compile("^(\\w+:)")

  def hasProtocol(url: String): Boolean = url.indexOf("://") != -1

  /**
    * @param url
    * @return The protocol of the URL, or None if failed.
    */
  def protocol(url: String): Option[String] = {
    url.indexOf("://") match {
      case -1 => None
      case index => Some(url.substring(0, index))
    }
  }

  /**
    * In order for a certain URL to be a root for
    * other relative URLs it must not end with a '/'.
    * @param url
    * @return
    */
  def makeRoot(url: String): String = {
    if (url.endsWith("/"))
      url.substring(0, url.length-1)
    else
      url
  }

  /**
    * The web pages to be crawled have to be either
    * http or https. If neither exists, then assume
    * http. If another protocol exists (e.g. ftp://)
    * then it will assume that it's not a valid URL.
    * @param url
    * @return
    */
  def correctUrl(url: String): Option[String] = {
    url.indexOf("://") match {
      case -1 =>
        if (noProtocolPattern.matcher(url).find() && endingPattern.matcher(url).find())
          Some("http://" + url)
        else
          None

      case index =>
        url.substring(0, index) match {
          case "http" | "https" => Some(url)
          case _ => None
        }
    }
  }

  /**
    * Normalizes a host by removing the first block
    * if found. Examples:
    * www.something.org -> something.org
    * forums.something.org -> something.org
    * something.org -> something.org
    * @param host
    * @return
    */
  def normalizeHost(host: String): String = {
    if (host.count(c => c == '.') == 1)
      host
    else
      host.substring(host.indexOf('.') + 1)
  }

  /**
    * Normalizes a URL by removing trailing '/'
    * if found.
    * @param url
    * @return
    */
  def normalizeUrl(url: String): String = url.last match {
    case '/' => url.substring(0, url.length-1)
    case _ => url
  }

  /**
    * Checks if a URL matches the pattern of supported
    * URLs.
    * @param url
    * @return
    */
  def isNotSupported(url: String): Boolean = notSupportedPattern.matcher(url).find()

  /**
    * Converts a URL which is relative to the directory
    * of the current page to an absolute path.
    * @param url
    * @param current The absolute path to the current page
    * @return
    */
  def directoryRelativeToAbsolute(url: String, current: String): Option[String] = {
    current.lastIndexOf('/') match {
      case -1 => None
      case index => Some(current.substring(0, index + 1) + url)
    }
  }

  /**
    * Retrieves the normalized host if the URL could
    * be parsed. Otherwise returns None
    * @param url
    * @return
    */
  def domain(url: String): Option[String] = {
    try {
      Some(normalizeHost(new URL(url).getHost))
    }
    catch {
      case _: Exception => None
    }
  }
}
