package org.scalawiki.time

import java.time.ZonedDateTime

case class TimeRange(start: Option[ZonedDateTime], end: Option[ZonedDateTime])
