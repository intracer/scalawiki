package org.scalawiki.desktop

import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafx.scene.control.ScrollPane
import scalafx.scene.layout.VBox
import scalafx.scene.web.WebView

object JavaFxBrowser extends JFXApp {
  stage = new PrimaryStage {
    title = "ScalaWiki"
    width = 500
    height = 500
    scene = new Scene {
      root = new VBox {
        children = Seq(new ScrollPane {
          content = new WebView {
            engine.loadContent("<b>Hello ScalaWiki</b>")
          }
        })
      }
    }
  }
}
