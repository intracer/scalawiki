package org.scalawiki.ui

import javax.swing.{SwingConstants, JProgressBar}

/** a JProgressBar displaying Messages
  * from https://github.com/ritschwumm/commonist/blob/master/src/main/scala/commonist/ui/StatusUI.scala
  * */
final class StatusUI extends JProgressBar(SwingConstants.HORIZONTAL) {
  setStringPainted(true)

  /** changes the upload progressbar to indeterminate state */
  def indeterminate(message:String, data:Object*) {
    setIndeterminate(true)
    setString(message)
  }

  /** changes the upload progressbar to determinate state */
  def determinate(message:String, value:Int, maximum:Int) {
    setIndeterminate(false)
    setString(message)
    setMaximum(maximum)
    setValue(value)
  }

  /** changes the upload progressbar to determinate state */
  def halt(message:String) {
    setIndeterminate(false)
    setString(message)
    setMaximum(0)
    setValue(0)
  }
}
