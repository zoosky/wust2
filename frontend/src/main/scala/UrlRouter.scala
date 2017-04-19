package wust.frontend

import rx._
import scalajs.js
import org.scalajs.dom.window
import org.scalajs.dom.raw.HashChangeEvent

object UrlRouter {
  val variable: RxVar[Option[String], Option[String]] = {
    val hash = RxVar[Option[String]](None)
    hash.foreach { hash =>
      val current = hash.getOrElse("")
      if (window.location.hash != current)
        window.location.hash = current
    }

    window.onhashchange = { ev: HashChangeEvent =>
      val current = Option(window.location.hash).filterNot(_.isEmpty).map(_.drop(1))
      if (hash.now != current)
        hash() = current
    }

    hash
  }
}