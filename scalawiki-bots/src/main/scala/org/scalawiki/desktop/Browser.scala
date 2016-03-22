package org.scalawiki.desktop

import java.awt.Desktop
import java.net.URI

object Browser {

  def browse(uri: String) = {
    if(Desktop.isDesktopSupported)
    {
      Desktop.getDesktop.browse(new URI(uri))
    }
  }

  def main(args: Array[String]) {
    browse("https://wikipedia.org")
  }

}
