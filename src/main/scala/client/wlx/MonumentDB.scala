package client.wlx

import client.wlx.dto.{Contest, Monument}
import client.wlx.query.MonumentQuery

class MonumentDB(val contest: Contest, monumentQuery: MonumentQuery) {

  var monuments: Seq[Monument] = Seq.empty

  var _byId: Map[String, Seq[Monument]] = Map.empty

  var _byRegion : Map[String, Seq[Monument]] = Map.empty

  def ids: Set[String] = _byId.keySet


  def fetchLists() = {
    monuments = monumentQuery.lists(contest.listTemplate)
    _byId = monuments.groupBy(_.id)
    _byRegion = monuments.groupBy(_.id.split("\\-")(0))

  }

  def byId(id: String) = _byId.getOrElse(id, Seq.empty[Monument]).headOption

  def byRegion(regId: String) = monuments.filter(_.id.startsWith(regId))


}
