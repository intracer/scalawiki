package client.wlx

import client.wlx.dto.{Contest, Monument}

class MonumentDB(val contest: Contest, val allMonuments: Seq[Monument]) {

  val monuments = allMonuments.filter(m => isIdCorrect(m.id))
  val wrongIdMonuments = allMonuments.filterNot(m => isIdCorrect(m.id))

  val _byId: Map[String, Seq[Monument]] = monuments.groupBy(_.id)

  val _byRegion : Map[String, Seq[Monument]] = monuments.groupBy(m => Monument.getRegionId(m.id))

  def ids: Set[String] = _byId.keySet

  def byId(id: String) = _byId.getOrElse(id, Seq.empty[Monument]).headOption

  def byRegion(regId: String) = _byRegion.getOrElse(regId, Seq.empty[Monument])

  def isIdCorrect(id:String) = {
    val idRegex = """(\d\d)-(\d\d\d)-(\d\d\d\d)"""
    id.matches(idRegex) && contest.country.regionIds.contains(Monument.getRegionId(id))
  }

}


