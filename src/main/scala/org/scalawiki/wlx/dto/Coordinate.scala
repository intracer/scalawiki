package org.scalawiki.wlx.dto

case class Coordinate(lat: String, lon: String) {
  def toOpenStreetMap =
    s"""<a target="_blank" href="http://www.openstreetmap.org/?mlat=$lat&amp;mlon=$lon&amp;zoom=14">$lat, $lon</a>"""
}

object Coordinate {
  def apply(latOpt:Option[String], lonOpt:Option[String]) = for (lat<-latOpt; lon <-lonOpt)
    yield new Coordinate(lat, lon)
}
