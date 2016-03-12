package org.scalawiki.edit

trait PageUpdateTask {
  def host: String
  def titles: Iterable[String]
  def updatePage(title: String, text: String): (String, String)
}
