package org.scalawiki.util

import scala.io.{Codec, Source}

object TestUtils {
  def resourceAsString(resource: String): String = {
    val is = getClass.getResourceAsStream(resource)
    Source.fromInputStream(is)(Codec.UTF8).mkString
  }
}
