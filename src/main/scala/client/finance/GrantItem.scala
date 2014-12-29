package client.finance

import squants.Money
import squants.market.USD


class GrantItem(
                 val number: String,
                 val description: String,
                 val unit: String,
                 val qty: String,
                 val costPerUnit: String,
                 val totalCost: Money,
                 val wmfContrib: Money,
                 val otherSources: Money,
                 val notes: String
                 ) {

  //  Number	Item description	Unit	Qty	Cost per unit UAH / USD	Total cost USD	WMF contribution (USD)	Other sources (USD)	Notes
  override def toString: String = s"$number $description - $wmfContrib"
}

object GrantItem {

//  def detailed()

  def apply(v: Seq[String]): GrantItem = {

    if (v.size == 9) {
      new GrantItem(v(0), v(1), v(2), v(3), v(4), toUSD(v(5)), toUSD(v(6)), toUSD(v(7)), v(8))
    } else {
      val parts = v(0).split(" ")
      new GrantItem(parts.head, parts.tail.mkString(" "), "", "", "", toUSD(v(1)), toUSD(v(2)), toUSD(v(3)), v(4))
    }
  }

  def toUSD(s: String): Money = {
    if (s.isEmpty)
      USD(0)
    else
      USD(s.toDouble)
  }


}