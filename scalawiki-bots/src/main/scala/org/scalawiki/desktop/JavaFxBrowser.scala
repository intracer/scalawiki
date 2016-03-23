package org.scalawiki.desktop

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control.ScrollPane
import scalafx.scene.layout.VBox
import scalafx.scene.web.WebView

trait JavaFxBrowserAppTrait extends JFXApp {

  def openStage() = {
    stage = new PrimaryStage {
      title = "ScalaWiki"
      scene = new Scene {
        root = new JavaFxBrowser().createRoot()
      }
    }
  }
}

class JavaFxBrowser {

  val webView = new WebView()

  def createRoot() = {
    new VBox {
      children = new ScrollPane {
        content = webView
        load(webView)
      }
    }
  }

  def load(browser: WebView) = {
    browser.engine.loadContent("<b>Hello ScalaWiki</b>")
  }
}

object JavaFxBrowser extends JavaFxBrowserAppTrait {

  openStage()

}