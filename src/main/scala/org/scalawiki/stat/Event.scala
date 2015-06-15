package org.scalawiki.stat

import org.joda.time.DateTime

class Event(
             val name: String,
             val start: DateTime,
             val end: DateTime) {


}

class ArticlesEvent(
                     name: String,
                     start: DateTime,
                     end: DateTime,
                     val newTemplate: String,
                     val improvedTemplate: String) extends Event(name, start, end) {


}

object Events {

  val WLEWeek = new ArticlesEvent(
    "Zaporizhia2-week",
    DateTime.parse("2015-05-04T00:00+02:00"),
    DateTime.parse("2015-05-31T00:00+02:00"),
    "Пам'ятки природи у Вікіпедії",
    "Пам'ятки природи у Вікіпедії")


  val Zaporizhia2Week = new ArticlesEvent(
    "Zaporizhia2-week",
    DateTime.parse("2015-02-12T00:00+02:00"),
    DateTime.parse("2015-02-27T00:00+02:00"),
    "Zaporizhia2-week-new",
    "Zaporizhia2-week-improve")

  val KhersonWeek = new ArticlesEvent(
    "Kherson-week",
    DateTime.parse("2015-01-06T00:00+02:00"),
    DateTime.parse("2015-01-28T00:00+02:00"),
    "Kherson-week-new",
    "Kherson-week-improve")



}