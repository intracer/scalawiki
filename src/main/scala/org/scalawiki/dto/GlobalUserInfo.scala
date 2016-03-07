package org.scalawiki.dto

import org.joda.time.DateTime

case class GlobalUserInfo(
                           home: String,
                           id: Long,
                           registration: DateTime,
                           name: String,
                           merged: Seq[SulAccount],
                           editCount: Long) {

}

case class SulAccount(
                       wiki: String,
                       url: String,
                       timestamp: DateTime,
                       method: String,
                       editCount: Long,
                       registration: DateTime)
