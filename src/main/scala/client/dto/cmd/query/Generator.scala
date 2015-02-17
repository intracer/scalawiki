package client.dto.cmd.query

import client.dto.cmd.query.list.ListArg

case class Generator[G](generator: ListArg) extends /*ArgWithParams[G, ListArg] with */ QueryParam[ListArg] {
  override def name: String = "generator"

  override def summary: String = ""

  override def pairs: Seq[(String, String)] =
    Seq(name -> generator.name) ++ generator.pairs.map {
      case (k,v) => ("g" + k, v)
    }
}
