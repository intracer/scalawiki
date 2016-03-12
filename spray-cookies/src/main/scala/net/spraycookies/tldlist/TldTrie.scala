package net.spraycookies.tldlist

private[tldlist] sealed trait TldTrie {
  def contains(list: List[String]): Boolean = {
    list match {
      case Nil ⇒ true
      case head :: tail ⇒ this match {
        case Leaf                ⇒ false
        case Wildcard(negations) ⇒ tail == Nil && !negations.contains(head)
        case Node(m) ⇒ m.get(head) match {
          case None           ⇒ false
          case Some(trieTail) ⇒ trieTail.contains(tail)
        }
      }
    }
  }
  def merge(that: TldTrie): TldTrie
}

private case class Node(map: Map[String, TldTrie]) extends TldTrie {
  def merge(that: TldTrie): TldTrie = {
    that match {
      case Node(thatMap) ⇒ Node(TldTrie.mapMerge(map, thatMap, (_: TldTrie).merge(_: TldTrie)))
      case Leaf          ⇒ this
      case x: Wildcard   ⇒ throw new Exception(s"tries $x and $this not mergeable")
    }
  }
}

private case object Leaf extends TldTrie {
  def merge(that: TldTrie) = that
}

private case class Wildcard(whitelist: Set[String]) extends TldTrie {
  def merge(that: TldTrie) = that match {
    case Leaf                    ⇒ this
    case Wildcard(thatWhitelist) ⇒ Wildcard(whitelist ++ thatWhitelist)
    case x: Node                 ⇒ throw new Exception(s"tries $x and $this not mergeable")
  }
}

private[tldlist] object TldTrie {

  def mapMerge[T, U](left: Map[T, U], right: Map[T, U], merge: (U, U) ⇒ U): Map[T, U] = {
    left.foldLeft(right)((nm, kvp) ⇒ {
      nm.get(kvp._1) match {
        case None       ⇒ nm + kvp
        case Some(val2) ⇒ nm + (kvp._1 -> merge(val2, kvp._2))
      }
    })
  }

  private def toTrie(elems: List[String]): TldTrie = {
    elems match {
      case head :: tail ⇒
        if (head == "*") Wildcard(Set.empty)
        else if (head.startsWith("!")) Wildcard(Set(head.substring(1)))
        else Node(Map(head -> toTrie(tail)))
      case Nil ⇒ Leaf
    }
  }

  def apply(domains: Iterator[String]): TldTrie = {
    domains.foldLeft(Leaf: TldTrie)((r, domain) ⇒ {
      val elems = domain.split('.').toList.reverse
      r.merge(toTrie(elems))
    })
  }
}