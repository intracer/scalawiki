package org.scalawiki.bots.stat

class NumericArrayStat(val name: String, val data: Seq[Long]) {
  val sum = data.sum
  val max = if (data.isEmpty) 0 else data.max
  val min = if (data.isEmpty) 0 else data.min
  val average = if (data.isEmpty) 0 else sum / data.size

  override def toString =
    s"$name $sum (max: $max, min: $min, average: $average)"
}
