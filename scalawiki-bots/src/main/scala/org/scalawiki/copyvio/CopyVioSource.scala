package org.scalawiki.copyvio

case class CopyVioSource(url: String,
                         confidence: Double,
                         violation: String,
                         skipped: Boolean) {
  def isSuspected = violation == "suspected"
}
