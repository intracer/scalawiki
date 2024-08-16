package org.scalawiki.json

import org.scalawiki.dto.Page

import scala.util.Try

trait Parser {

  def parse(str: String): Try[Seq[Page]]

}
