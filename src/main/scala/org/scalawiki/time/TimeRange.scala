package org.scalawiki.time

import org.joda.time.DateTime

case class TimeRange(start: Option[DateTime], end: Option[DateTime])