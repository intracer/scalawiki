package org.scalawiki.wiremock

import com.github.tomakehurst.wiremock.matching.{ContentPattern, MatchResult}

class BodyParamsMatcher(params: Map[String, String]) extends ContentPattern[String](params.toString) {
  override def getName: String = "BodyParamsMatcher"

  override def getExpected: String = params.toString()

  override def `match`(value: String): MatchResult = {
    val valueParams = value.split("&").map(_.split("=")).map(x => x(0) -> x(1)).toMap

    MatchResult.of(valueParams == params)
  }
}
