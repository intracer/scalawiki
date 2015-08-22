package org.scalawiki.dto.cmd.query

import org.scalawiki.dto.cmd.EnumArg

/**
 * Get the list of pages to work on by executing the specified query module
 * @param generator
 */

trait GeneratorArg extends EnumArg[GeneratorArg]

case class Generator(generator: EnumArg[GeneratorArg]) extends /*ArgWithParams[G, ListArg] with */ QueryParam[EnumArg[GeneratorArg]] {
  override def name: String = "generator"

  override def summary: String = ""

  override def pairs: Seq[(String, String)] =
    Seq(name -> generator.name) ++ generator.pairs.map {
      case (k,v) => ("g" + k, v)
    }
}
