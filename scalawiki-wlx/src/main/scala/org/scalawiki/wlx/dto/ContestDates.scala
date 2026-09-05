package org.scalawiki.wlx.dto

import com.typesafe.config.Config

import java.time.{LocalDate, ZoneOffset, ZonedDateTime}
import scala.util.Try

/** Key calendar dates for one contest year, read from the `dates.<year>` block of
  * the campaign `.conf` (see `wlm_ua.conf` / `wle_ua.conf`). Every field is
  * optional: a missing block or key means "not recorded, fall back to a default".
  *
  * The on-wiki rules state times in Kyiv time; each date here is treated as
  * end-of-day UTC — a <=3h skew that never matters for the cache-freshness and
  * eligibility checks that consume it.
  *
  * @param uploadStart
  *   first day of the on-wiki submission window (Регламент п. 5.2)
  * @param uploadEnd
  *   last day of the submission window; used as the "cache was accurate until"
  *   cut-off when `--csv-cache-resync` re-checks a frozen past year
  * @param latestAllowedPicturedDate
  *   photos for the main nominations must be created on or before this date
  *   (Регламент п. 9.1 / п. 5.2 — a wartime security rule). Absent for the
  *   pre-2022 years, which had no such restriction.
  */
case class ContestDates(
    uploadStart: Option[LocalDate] = None,
    uploadEnd: Option[LocalDate] = None,
    latestAllowedPicturedDate: Option[LocalDate] = None
) {

  private def endOfDayUtc(d: LocalDate): ZonedDateTime =
    d.atTime(23, 59, 59).atZone(ZoneOffset.UTC)

  def uploadEndInstant: Option[ZonedDateTime] = uploadEnd.map(endOfDayUtc)

  def latestAllowedPicturedInstant: Option[ZonedDateTime] =
    latestAllowedPicturedDate.map(endOfDayUtc)
}

object ContestDates {

  private def dateAt(config: Config, key: String): Option[LocalDate] =
    if (config.hasPath(key)) Try(LocalDate.parse(config.getString(key))).toOption
    else None

  def fromConfig(config: Config): ContestDates =
    ContestDates(
      uploadStart = dateAt(config, "upload-start"),
      uploadEnd = dateAt(config, "upload-end"),
      latestAllowedPicturedDate = dateAt(config, "latest-allowed-pictured-date")
    )
}
