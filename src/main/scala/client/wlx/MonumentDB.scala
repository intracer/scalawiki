package client.wlx

import client.wlx.dto.{Region, Contest, Monument}
import client.wlx.query.{MonumentQueryCached, MonumentQueryPickling, MonumentQueryApi, MonumentQuery}

class MonumentDB(val contest: Contest, monumentQuery: MonumentQuery) {

  var monuments: Seq[Monument] = Seq.empty

  var _byId: Map[String, Seq[Monument]] = Map.empty

  var _byRegion : Map[String, Seq[Monument]] = Map.empty

  def ids: Set[String] = _byId.keySet


  def fetchLists() = {
    val all = monumentQuery.lists(contest.listTemplate)
    monuments = all.filter(m => isIdCorrect(m.id))
    val wrongIds = all.filterNot(m => isIdCorrect(m.id))

    _byId = monuments.groupBy(_.id)
    _byRegion = monuments.groupBy(_.id.split("\\-")(0))
  }

  def byId(id: String) = _byId.getOrElse(id, Seq.empty[Monument]).headOption

  def byRegion(regId: String) = monuments.filter(_.id.startsWith(regId))

  def isIdCorrect(id:String) = {
    val idRegex = """(\d\d)-(\d\d\d)-(\d\d\d\d)"""
    id.matches(idRegex) && Region.Ukraine.contains(id.split("\\-")(0))
  }

}


object MonumentDB {

  def create(contest: Contest, caching: Boolean = true, pickling: Boolean = false) = {
    val api = new MonumentQueryApi(contest)

    val query = if (caching)
      new MonumentQueryCached(
        if (pickling)
          new MonumentQueryPickling(api, contest)
        else api
      )
    else api

    new MonumentDB(contest, query)
  }

}