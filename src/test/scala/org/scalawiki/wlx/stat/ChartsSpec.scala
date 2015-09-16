package org.scalawiki.wlx.stat

import org.specs2.mutable.Specification

import scala.collection.JavaConverters._

class ChartsSpec extends Specification {

  "createTotalDataset" should {
    "one year" in {
      val charts = new Charts

      val years = Seq(2012)
      val values = Seq(8000)
      val dataset = charts.createTotalDataset(years, values)

      dataset.getColumnKeys.asScala === Seq("Всього")
      dataset.getRowKeys.asScala === years
      dataset.getValue(2012, "Всього") === 8000
    }

    "4 years" in {
      val charts = new Charts

      val years = Seq(2012, 2013, 2014, 2015)
      val values = Seq(8000, 10000, 14000, 20000)
      val dataset = charts.createTotalDataset(years, values)

      dataset.getColumnKeys.asScala === Seq("Всього")
      dataset.getRowKeys.asScala === years

      years.indices.map { i =>
        dataset.getValue(years(i), "Всього") === values(i)
      }
    }
  }

  "intersectionDataSet" should {
    "1 year" in {
      val charts = new Charts

      val years = Seq(2012)
      val ids = Seq(Set("1", "2", "3"))

      val dataset = charts.intersectionDataSet(years, ids)

      dataset.getKeys.asScala === Seq("2012")
      dataset.getValue("2012") === 3
    }

    "2 years in" in {
      val charts = new Charts

      val years = Seq(2012, 2013)
      val ids = Seq(
        Set("1", "2", "3"),
        Set("2", "3", "4", "5", "6")
      )

      val dataset = charts.intersectionDataSet(years, ids)

      dataset.getKeys.asScala === Seq("2012", "2012 & 2013", "2013")
      dataset.getValue("2012") === 1
      dataset.getValue("2012 & 2013") === 2
      dataset.getValue("2013") === 3
    }

    "3 years in" in {
      val charts = new Charts

      val years = Seq(2012, 2013, 2014)

      val twoAndThree = (1 to 6).map("2 & 3 " + _)
      val twoAndFour = (1 to 8).map("2 & 4 " + _)
      val threeAndFour = (1 to 12).map("3 & 4 " + _)
      val intersection = (1 to 24).map("2 & 3 & 4 " + _)

      val ids = Seq(
        Set("2_only") ++ twoAndThree ++ twoAndFour ++ intersection,
        Set("3_only1", "3_only2") ++ twoAndThree ++ threeAndFour ++ intersection,
        Set("4_only1", "4_only2", "4_only3") ++ twoAndFour ++ threeAndFour ++ intersection
      )

      val dataset = charts.intersectionDataSet(years, ids)

      dataset.getKeys.asScala === Seq(
        "2012", "2012 & 2013", "2012 & 2014",
        "2013", "2013 & 2014",
        "2014",
        "2012 & 2013 & 2014")

      dataset.getValue("2012") === 1
      dataset.getValue("2012 & 2013") === 6
      dataset.getValue("2012 & 2014") === 8
      dataset.getValue("2013") === 2
      dataset.getValue("2013 & 2014") === 12
      dataset.getValue("2014") === 3
      dataset.getValue("2012 & 2013 & 2014") === 24
    }

    "4 years in" in {
      val charts = new Charts

      val years = Seq(2012, 2013, 2014, 2015)

      val twoOnly = (1 to 2).map("2 only " + _).toSet
      val twoAndThree = (1 to 6).map("2 & 3 " + _)
      val twoAndFour = (1 to 8).map("2 & 4 " + _)
      val twoAndFive = (1 to 10).map("2 & 5 " + _)

      val threeOnly = (1 to 3).map("3 only " + _).toSet
      val threeAndFour = (1 to 12).map("3 & 4 " + _)
      val threeAndFive = (1 to 15).map("3 & 5 " + _)

      val fourOnly = (1 to 4).map("4 only " + _).toSet
      val fourAndFive = (1 to 20).map("4 & 5 " + _)

      val fiveOnly = (1 to 5).map("5 only " + _).toSet

      val except2 = (1 to 60).map("3 & 4 & 5" + _)
      val except3 = (1 to 40).map("2 & 4 & 5" + _)
      val except4 = (1 to 30).map("2 & 3 & & 5" + _)
      val except5 = (1 to 24).map("2 & 3 & 4 " + _)

      val intersection = (1 to 120).map("2 & 3 & 4 & 5" + _)

      val ids = Seq(
        twoOnly ++ twoAndThree ++ twoAndFour ++ twoAndFive ++ intersection ++ except3 ++ except4 ++ except5,
        threeOnly ++ twoAndThree ++ threeAndFour ++ threeAndFive ++ intersection ++ except2 ++ except4 ++ except5,
        fourOnly ++ twoAndFour ++ threeAndFour ++ fourAndFive ++ intersection ++ except2 ++ except3 ++ except5,
        fiveOnly ++ twoAndFive ++ threeAndFive ++ fourAndFive ++ intersection ++ except2 ++ except3 ++ except4
      )

      val dataset = charts.intersectionDataSet(years, ids)

      dataset.getKeys.asScala === Seq(
        "2012", "2012 & 2013", "2012 & 2014", "2012 & 2015",
        "2013", "2013 & 2014", "2013 & 2015",
        "2014", "2014 & 2015",
        "2015",
        "2013 & 2014 & 2015",
        "2012 & 2014 & 2015",
        "2012 & 2013 & 2015",
        "2012 & 2013 & 2014",
        "2012 & 2013 & 2014 & 2015")

      dataset.getValue("2012") === 2
      dataset.getValue("2012 & 2013") === 6
      dataset.getValue("2012 & 2014") === 8
      dataset.getValue("2012 & 2015") === 10
      dataset.getValue("2013") === 3
      dataset.getValue("2013 & 2014") === 12
      dataset.getValue("2013 & 2015") === 15
      dataset.getValue("2014") === 4
      dataset.getValue("2014 & 2015") === 20
      dataset.getValue("2015") === 5
      dataset.getValue("2012 & 2013 & 2014 & 2015") === 120

      dataset.getValue("2013 & 2014 & 2015") === 60
      dataset.getValue("2012 & 2014 & 2015") === 40
      dataset.getValue("2012 & 2013 & 2015") === 30
      dataset.getValue("2012 & 2013 & 2014") === 24

//      val except2 = (1 to 60).map("3 & 4 & 5" + _)
//      val except3 = (1 to 40).map("2 & 4 & 5" + _)
//      val except4 = (1 to 30).map("2 & 3 & & 5" + _)
//      val except5 = (1 to 24).map("2 & 3 & 4 " + _)

    }
  }

}
