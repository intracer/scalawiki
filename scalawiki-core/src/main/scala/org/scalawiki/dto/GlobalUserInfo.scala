package org.scalawiki.dto

import java.time.ZonedDateTime

case class GlobalUserInfo(
    home: String,
    id: Long,
    registration: ZonedDateTime,
    name: String,
    merged: Seq[SulAccount],
    editCount: Long
)

case class SulAccount(
    wiki: String,
    url: String,
    timestamp: ZonedDateTime,
    method: String,
    editCount: Long,
    registration: ZonedDateTime
)
