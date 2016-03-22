package org.scalawiki.desktop

import javafx.application.Application
import javafx.scene.{Group, Scene}
import javafx.scene.control.ScrollPane
import javafx.scene.layout.VBox
import javafx.scene.web.WebView
import javafx.stage.Stage

class JavaFxBrowser extends Application {

  override def start(stage: Stage) {
    stage.setTitle("HTML")
    stage.setWidth(500)
    stage.setHeight(500)
    val scene = new Scene(new Group())

    val root = new VBox()

    val browser = new WebView()
    val webEngine = browser.getEngine

    val scrollPane = new ScrollPane()
    scrollPane.setContent(browser)
    webEngine.loadContent("<b>Hello ScalaWiki</b>")

    root.getChildren.addAll(scrollPane)
    scene.setRoot(root)

    stage.setScene(scene)
    stage.show()
  }
}

object JavaFxBrowser {
  def main(args: Array[String]) {
    Application.launch(classOf[JavaFxBrowser], args: _*)
  }
}

