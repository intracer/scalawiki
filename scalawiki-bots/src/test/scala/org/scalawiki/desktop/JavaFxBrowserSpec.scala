package org.scalawiki.desktop

import javafx.scene.Parent

import org.loadui.testfx.GuiTest
import org.specs2.mutable.Specification

class JavaFxBrowserSpec extends Specification {

  var browser: JavaFxBrowser = _

  "browser" should {

    "contain html" in {

      val browserRoot: (() => Parent) = { () =>
        browser = new JavaFxBrowser()
        browser.createRoot()
      }

      val guiTest = new ScalaGuiTest(browserRoot)
      guiTest.setupStage()

      val html = browser.webView.engine.getDocument.getDocumentElement.getTextContent
      html === "Hello ScalaWiki"
    }
  }
}


class ScalaGuiTest(val root: () => Parent) extends GuiTest {
  override def getRootNode: Parent = root()

  def getStage = GuiTest.stage
}


