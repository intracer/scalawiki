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

  def compose(g: F => T): Map[T, Grouping[T, F]] =
    grouped.mapValues(v => new Grouping("", g, v))

}

object ImageGrouping {

  def byMpx = (i: Image) => i.mpx.map(_.toInt).getOrElse(-1)

  def byMonument = (i: Image) => i.monumentId.getOrElse("")

  def byRegion = (i: Image) => Monument.getRegionId(i.monumentId)

  def byAuthor = (i: Image) => i.author.getOrElse("")

  }

