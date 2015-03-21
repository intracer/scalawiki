package org.scalawiki.dto.cmd.query

import org.scalawiki.dto.cmd.query.list.ListArg

/**
 * Get the list of pages to work on by executing the specified query module
 * @param generator
 */
case class Generator(generator: ListArg) extends /*ArgWithParams[G, ListArg] with */ QueryParam[ListArg] {
  override def name: String = "generator"

  override def summary: String = ""

  override def pairs: Seq[(String, String)] =
    Seq(name -> generator.name) ++ generator.pairs.map {
      case (k,v) => ("g" + k, v)
    }
}
