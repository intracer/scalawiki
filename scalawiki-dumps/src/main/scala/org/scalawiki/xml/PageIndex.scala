package org.scalawiki.xml

case class PageIndex(offset: Long, id: Long, title: String) {

  override def toString = s"$offset:$id:$title"

}

object PageIndex {

  def fromString(s: String) = {
    val arr = s.split(":")
    PageIndex(
      offset = arr(0).toLong,
      id = arr(1).toLong,
      title = arr.slice(2, arr.length).mkString(":")
    )
  }

}
