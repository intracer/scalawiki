package org.scalawiki.desktop

import javafx.scene.Parent

import org.loadui.testfx.GuiTest
import org.specs2.mutable.Specification

class JavaFxBrowserSpec extends Specification {

  var browser: JavaFxBrowser = _

  def makeHeadLess() = {
    System.setProperty("testfx.robot", "glass")
    System.setProperty("testfx.headless", "true")
    System.setProperty("prism.order", "sw")
    System.setProperty("prism.text", "t2k")
  }

  "browser" should {

    "contain html" in {

      makeHeadLess()

      val browserRoot: (() => Parent) = { () =>
        browser = new JavaFxBrowser()
        browser.createRoot()
      }

      val guiTest = new ScalaGuiTest(browserRoot)
      guiTest.internalSetup()

      val html = browser.webView.engine.getDocument.getDocumentElement.getTextContent
      html === "Hello ScalaWiki"
    }
  }
}

class ScalaGuiTest(val root: () => Parent) extends GuiTest {
  override def getRootNode: Parent = root()
}


