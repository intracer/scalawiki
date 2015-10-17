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

  val ArchWeek = new ArticlesEvent(
    "Вікіпедія:Проект:Тематичний тиждень/Тиждень архітектури",
    DateTime.parse("2015-01-21T00:00+02:00"),
    DateTime.parse("2015-02-12T00:00+02:00"),
    "Architecture-week-new",
    "Architecture-week-improve")

  val WLMContest = new ArticlesEvent(
    ":wmua:Вікіпедія любить пам'ятки",
    DateTime.parse("2015-03-05T00:00+02:00"),
    DateTime.parse("2015-04-06T00:00+03:00"),
    "Вікіпедія любить пам'ятки",
    "Вікіпедія любить пам'ятки")

  val WLEContest = new ArticlesEvent(
    ":wmua:Пам'ятки природи у Вікіпедії",
    DateTime.parse("2015-05-04T00:00+02:00"),
    DateTime.parse("2015-06-01T00:00+03:00"),
    "Пам'ятки природи у Вікіпедії",
    "Пам'ятки природи у Вікіпедії")

  val CEESpring = new ArticlesEvent(
    "CEE Spring 2015",
    DateTime.parse("2015-03-23T00:00+02:00"),
    DateTime.parse("2015-06-01T00:00+03:00"),
    "CEE Spring 2015",
    "CEE Spring 2015")

  val CrimeaContest = new ArticlesEvent(
    ":wmua:Пам'ятки України: Крим",
    DateTime.parse("2015-06-26T00:00+02:00"),
    DateTime.parse("2015-09-01T00:00+03:00"),
    "Пам'ятки України: Крим — конкурсна",
    "Пам'ятки України: Крим — конкурсна")

  val CrimeaWeek = new ArticlesEvent(
    "Вікіпедія:Проект:Тематичний тиждень/Кримський місяць",
    DateTime.parse("2015-06-26T00:00+02:00"),
    DateTime.parse("2015-09-01T00:00+03:00"),
    "Кримський місяць — позаконкурсна, створена",
    "Кримський місяць — позаконкурсна, покращена")


  val Zaporizhia2Week = new ArticlesEvent(
    "Вікіпедія:Проект:Тематичний тиждень/Тиждень Запорізької області 2",
    DateTime.parse("2015-02-12T00:00+02:00"),
    DateTime.parse("2015-02-27T00:00+02:00"),
    "Zaporizhia2-week-new",
    "Zaporizhia2-week-improve")

  val KhersonWeek = new ArticlesEvent(
    "Вікіпедія:Проект:Тематичний тиждень/Тиждень Херсонщини",
    DateTime.parse("2015-01-06T00:00+02:00"),
    DateTime.parse("2015-01-28T00:00+02:00"),
    "Kherson-week-new",
    "Kherson-week-improve")

  val MuseumWeek2 = new ArticlesEvent(
    "Вікіпедія:Проект:Тематичний тиждень/Другий музейний тиждень",
    DateTime.parse("2015-03-07T00:00+02:00"),
    DateTime.parse("2015-03-25T00:00+02:00"),
    "Museum-week-new",
    "Museum-week-improve")

  val UaFootballWeek = new ArticlesEvent(
    "Вікіпедія:Проект:Тематичний тиждень/Місяць українського футболу",
    DateTime.parse("2015-03-23T00:00+02:00"),
    DateTime.parse("2015-04-24T00:00+03:00"),
    "Ua-football-week-new",
    "Ua-football-week-improve")

  val geographyWeek = new ArticlesEvent(
    "Вікіпедія:Проект:Тематичний тиждень/Тиждень географії",
    DateTime.parse("2015-04-27T00:00+02:00"),
    DateTime.parse("2015-05-18T00:00+03:00"),
    "geography-week-new",
    "geography-week-improve")

  val BashkortostanWeek = new ArticlesEvent(
    "Вікіпедія:Проект:Тематичний тиждень/Тиждень Башкортостану",
    DateTime.parse("2015-06-03T00:00+02:00"),
    DateTime.parse("2015-06-17T00:00+03:00"),
    "Bashkortostan-week-new",
    "Bashkortostan-week-improve")

  val astronomyWeek = new ArticlesEvent(
    "Вікіпедія:Проект:Тематичний тиждень/Астрономічний тиждень",
    DateTime.parse("2015-06-15T00:00+02:00"),
    DateTime.parse("2015-03-29T00:00+03:00"),
    "astronomy-week-new",
    "astronomy-week-improve")

  val ecologyWeek = new ArticlesEvent(
    "Вікіпедія:Проект:Тематичний тиждень/Тиждень екології",
    DateTime.parse("2015-06-17T00:00+02:00"),
    DateTime.parse("2015-07-01T00:00+03:00"),
    "ecology-week-new",
    "ecology-week-new")

  val NarodyWeek = new ArticlesEvent(
    "Narody-week",
    DateTime.parse("2015-07-01T00:00+02:00"),
    DateTime.parse("2015-07-16T00:00+03:00"),
    "Narody-week-new",
    "Narody-week-improve")

  val BiologyWeek2 = new ArticlesEvent(
    "Biology-week2-new",
    DateTime.parse("2015-07-06T00:00+02:00"),
    DateTime.parse("2015-08-17T00:00+03:00"),
    "Biology-week2-new",
    "Biology-week2-improve")

  val brotherWeek = new ArticlesEvent(
    "Вікіпедія:Проект:Тематичний тиждень/Назустріч братам (українсько-білоруський тиждень)",
    DateTime.parse("2015-08-29T00:00+02:00"),
    DateTime.parse("2015-09-12T00:00+03:00"),
    "brother-week-new",
    "brother-week-improve")

  val allContests = Seq(
    WLMContest, CEESpring, WLEContest, CrimeaContest
  )

  val allWeeks = Seq(
    KhersonWeek, ArchWeek, Zaporizhia2Week, MuseumWeek2, UaFootballWeek,
    geographyWeek, BashkortostanWeek, astronomyWeek, ecologyWeek, CrimeaWeek,
    NarodyWeek, BiologyWeek2, brotherWeek
  )

}