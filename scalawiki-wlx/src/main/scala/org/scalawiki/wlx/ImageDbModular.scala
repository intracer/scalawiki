package org.scalawiki.wlx

import org.scalawiki.dto.Image
import org.scalawiki.wlx.dto.{Contest, Monument}


class ImageDbModular(val contest: Contest, val images: Seq[Image],
                     val monumentDb: Option[MonumentDB],
                     val oldMonumentDb: Option[MonumentDB] = None) {

  def groupBy[S](f: Image => S) = {
    images.groupBy(f)
  }

}

class Grouping[T, F](name: String, val f: F => T, data: Seq[F]) {

  val grouped: Map[T, Seq[F]] = data.groupBy(f)

  val keys: Set[T] = grouped.keySet

  def by(key: T): Seq[F] = grouped.getOrElse(key, Seq.empty)

  def headBy(key: T): F = by(key).head

  def headOptionBy(key: T): Option[F] = by(key).headOption

  def compose(g: F => T): NestedGrouping[T, F] =
    new NestedGrouping(
      grouped.mapValues(v => new Grouping("", g, v))
    )

}

class NestedGrouping[T, F](val grouped: Map[T, Grouping[T, F]]) {

  val keys: Set[T] = grouped.keySet

  def by(key: T): Grouping[T, F] = grouped.getOrElse(key, new Grouping[T, F]("", null, Seq.empty))

  def by(key1: T, key2: T): Seq[F] = by(key1).by(key2)

  def headBy(key1: T, key2: T): F = by(key1).headBy(key2)

  def headOptionBy(key1: T, key2: T): Option[F] = by(key1).headOptionBy(key2)

}


object ImageGrouping {

  def byMpx = (i: Image) => i.mpx.map(_.toInt).getOrElse(-1)

  def byMonument = (i: Image) => i.monumentId.getOrElse("")

  def byRegion = (i: Image) => Monument.getRegionId(i.monumentId)

  def byAuthor = (i: Image) => i.author.getOrElse("")

}

