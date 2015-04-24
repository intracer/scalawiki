package org.scalawiki.ui

//import java.awt.Dimension

import java.awt.{BorderLayout, Dimension}
import javax.swing.{WindowConstants, JSplitPane, JPanel, JFrame}


class MainWindow(title: String) {

  private val statusUI = new StatusUI
  private val commonUI = new JPanel
  private val directoryUI = new JPanel
  private val uploadUI = new JPanel
  private val imageListUI = new JPanel

  private val commonPanel = new JPanel
  commonPanel setLayout new BorderLayout
  commonPanel add(commonUI, BorderLayout.NORTH)
  commonPanel add(directoryUI, BorderLayout.CENTER)

  private val mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, commonPanel, imageListUI)
  mainSplit setResizeWeight 0

  private val uploadPanel = new JPanel
  uploadPanel setLayout new BorderLayout
  uploadPanel add(statusUI, BorderLayout.CENTER)
  uploadPanel add(uploadUI, BorderLayout.EAST)

  private val windowPanel = new JPanel
  windowPanel setLayout new BorderLayout
  windowPanel add(mainSplit, BorderLayout.CENTER)
  windowPanel add(uploadPanel, BorderLayout.SOUTH)

  val menubar = new MainMenu

  val window = new JFrame(title)
  window setDefaultCloseOperation WindowConstants.EXIT_ON_CLOSE
  window.getContentPane add windowPanel
  window setJMenuBar menubar.peer

  window pack()

  window setSize new Dimension(800, 600)
  window setLocationRelativeTo null
  window setVisible true


}

object MainWindow {

  def main(args: Array[String]) {
    val window = new MainWindow("ScalaWiki")


  }
}
