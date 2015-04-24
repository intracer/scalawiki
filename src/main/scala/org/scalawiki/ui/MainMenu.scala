package org.scalawiki.ui

import javax.swing.KeyStroke

import scala.swing._

class MainMenu extends MenuBar {

  contents += new Menu("File") {
    contents += new MenuItem(new Action("Open") {
      accelerator = Some(KeyStroke.getKeyStroke("ctrl O"))

      def apply() {
        val chooser = new FileChooser
        val result = chooser.showOpenDialog(null)
        if (result == FileChooser.Result.Approve) {
          println("Approve -- " + chooser.selectedFile)
          Some(chooser.selectedFile)
        } else None
      }
    })
    contents += new Separator
    contents += new MenuItem(new Action("Exit") {
      def apply() {
        sys.exit(0)
      }
    })
  }

}
