package org.scalawiki.bots.museum

case class Diff[T](name: String, before: T, after: T)

object Differ {

  def diff(orig: Product, update: Product): Map[Int, Any] = {
    assert(orig != null && update != null, "Both products must be non-null")
    assert(orig.getClass == update.getClass, "Both products must be of the same class")

    val names = orig.getClass.getDeclaredFields.map(_.getName)

    val diffs = for (ix <- 0 until orig.productArity) yield {
      (orig.productElement(ix), update.productElement(ix)) match {
        case (p1: Product, p2: Product) if p1 != p2 => Some(ix -> diff(p1, p2))
        case (x, y) if x != y => Some(ix -> Diff(names(ix), x, y))
        case _ => None
      }
    }

    diffs.flatten.toMap
  }
}