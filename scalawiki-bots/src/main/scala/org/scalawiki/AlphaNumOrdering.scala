package org.scalawiki

import net.sf.uadetector.internal.util.AlphanumComparator

object AlphaNumOrdering extends Ordering[String] {
  val comparator = new AlphanumComparator()
  def compare(a: String, b: String) = comparator.compare(a, b)
}
