package org.scalawiki.desktop

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control.ScrollPane
import scalafx.scene.layout.VBox
import scalafx.scene.web.WebView

trait JavaFxBrowser extends JFXApp {

  def load(browser: WebView)

  def openStage() = {
    val browser = new WebView()
    stage = new PrimaryStage {
      title = "ScalaWiki"
      scene = new Scene {
        root = new VBox {
          children = Seq(new ScrollPane {
            content = browser
            load(browser)
          })
        }
      }
    }
  }
}

object JavaFxBrowserMain extends JavaFxBrowser {

  openStage()

  override def load(browser: WebView) = {
    browser.engine.loadContent("<b>Hello ScalaWiki</b>")
  }
}