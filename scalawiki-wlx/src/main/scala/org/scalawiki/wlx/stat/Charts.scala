package org.scalawiki.wlx.stat

import java.awt.{Color, Font}
import java.io.File
import java.text.DecimalFormat

import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.labels.StandardPieSectionLabelGenerator
import org.jfree.chart.plot.{PiePlot, PlotOrientation}
import org.jfree.chart.renderer.category.{BarRenderer, StandardBarPainter}
import org.jfree.chart.{ChartFactory, ChartUtilities, JFreeChart}
import org.jfree.data.category.{CategoryDataset, DefaultCategoryDataset}
import org.jfree.data.general.{DefaultPieDataset, PieDataset}
import org.scalawiki.{MwBot, WithBot}

class Charts extends WithBot {

  override def host: String = MwBot.commons

  val color2014 = new Color(220, 57, 18)
  val color2013 = new Color(255, 153, 0)
  val color2012 = new Color(51, 102, 204)

  def saveAsJPEG(chart: JFreeChart, filename: String, width: Int, height: Int) {
    ChartUtilities.saveChartAsJPEG(new File(filename), chart, width, height)
  }

  def saveAsPNG(chart: JFreeChart, filename: String, width: Int, height: Int) {
    ChartUtilities.saveChartAsPNG(new File(filename), chart, width, height)
  }

  /**
   * Creates a chart.
   *
   * @param dataset  the dataset.
   *
   * @return A chart.
   */
  def createPieChart(dataset: PieDataset, title: String) = {

    val chart = ChartFactory.createPieChart(
      title, // chart title
      dataset, // data
      false, // include legend
      true,
      false
    )


    val plot = chart.getPlot.asInstanceOf[PiePlot]
    plot.setLabelFont(new Font("SansSerif", Font.BOLD, 14))
    plot.setNoDataMessage("No data available")
    plot.setCircular(true)
    plot.setLabelGap(0.02)
    plot.setShadowXOffset(0)
    plot.setShadowYOffset(0)
    plot.setBackgroundPaint(Color.white)

    plot.setSectionPaint("2013", color2012)
    plot.setSectionPaint("2013 & 2014", blend(color2012, color2013))
    plot.setSectionPaint("2014", color2013)
    plot.setSectionPaint("2014 & 2015", blend(color2013, color2014))
    plot.setSectionPaint("2015", color2014)
    plot.setSectionPaint("2013 & 2015", blend(color2012, color2014))
    plot.setSectionPaint("2013 & 2014 & 2015", new Color(0x99CC00))

    val gen = new StandardPieSectionLabelGenerator("{0}:\n{1} ({2})", new DecimalFormat("0"), new DecimalFormat("0%"))
    plot.setLabelGenerator(gen)
    chart
  }

  def blend(c0: Color, c1: Color, weight: Double = 0.5) = {
    val r = (c0.getRed + c1.getRed) * weight
    val g = (c0.getGreen + c1.getGreen) * weight
    val b = (c0.getBlue + c1.getBlue) * weight

    new Color(r.toInt, g.toInt, b.toInt)
  }

  /**
   * Returns a sample dataset.
   *
   * @return The dataset.
   */
  def createTotalDataset(years: Seq[Int], values: Seq[Int]) = {

    val category1 = "Всього"

    val dataset = new DefaultCategoryDataset()

    years.zip(values).foreach { case (year, value) => dataset.addValue(value, year, category1) }

    dataset
  }


  /**
   * Creates a sample chart.
   *
   * @param dataset  the dataset.
   *
   * @return The chart.
   */
  def createChart(dataset: CategoryDataset, rangeAxisLabel: String) = {

    //ChartFactory.setChartTheme(StandardChartTheme.createDarknessTheme());

    // create the chart...
    val chart = ChartFactory.createBarChart(
      "", // chart title
      rangeAxisLabel, // domain axis label
      "Сфотографовано пам'яток", // range axis label
      dataset, // data
      PlotOrientation.HORIZONTAL, // orientation
      true, // include legend
      true, // tooltips?
      false // URLs?
    )

    // set the background color for the chart...
    //    chart.setBackgroundPaint(Color.white)

    // get a reference to the plot for further customisation...
    val plot = chart.getCategoryPlot
    plot.setBackgroundPaint(Color.white)
    //    plot.setDomainGridlinePaint(Color.white)
    //    plot.setRangeGridlinePaint(Color.white)
    plot.getRenderer.asInstanceOf[BarRenderer].setBarPainter(new StandardBarPainter())

    plot.setDomainGridlinePaint(Color.lightGray)
    plot.setRangeGridlinePaint(Color.lightGray)

    // set the range axis to display integers only...
    val rangeAxis = plot.getRangeAxis.asInstanceOf[NumberAxis]
    rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits())

    // disable bar outlines...
    val renderer = plot.getRenderer.asInstanceOf[BarRenderer]
    renderer.setDrawBarOutline(true)

    renderer.setSeriesPaint(0, color2012)
    renderer.setSeriesPaint(1, color2013)
    renderer.setSeriesPaint(2, color2014)

    //    val domainAxis = plot.getDomainAxis
    //    domainAxis.setCategoryLabelPositions(
    //      CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0)
    //    )

    chart
  }

  // up to 3 years
  def intersectionDiagram(title: String, filename: String, years: Seq[Int], idsSeq: Seq[Set[String]], width: Int, height: Int) {
    val pieDataset = intersectionDataSet(years, idsSeq)

    val pieChart = createPieChart(pieDataset, title)
    saveCharts(pieChart, filename, width, height)
  }


  def intersectionDataSet(years: Seq[Int], idsSeq: Seq[Set[String]]): DefaultPieDataset = {
    require(years.nonEmpty, "years should not be empty")
    require(idsSeq.nonEmpty, "idsSeq should not be empty")
    require(years.size == idsSeq.size, "years and idsSeq should have same size")

    val intersectAll = idsSeq.reduce(_ intersect _)
    val unionAll = idsSeq.reduce(_ ++ _)

    val yearsCounts = unionAll.map { id => id -> idsSeq.count(_.contains(id)) }.toMap

    val only = idsSeq.zipWithIndex.map {
      case (ids, i) => ids -- removeByIndex(idsSeq, i).foldLeft(Set.empty[String])(_ ++ _)
    }

    val idsMap = unionAll.map {
      id =>
        id -> years.zip(idsSeq).flatMap {
          case (year, set) =>
            if (set.contains(id)) Some(year) else None
        }
    }.toMap

    val yearsMap = idsMap.view.mapValues(_.mkString(" & ")).groupBy(_._2).view.mapValues(_.map(_._1))

    val pieDataset = new DefaultPieDataset()

    years.zipWithIndex.foreach {
      case (year, i) =>

        pieDataset.setValue(year.toString, only(i).size)

        (i + 1).until(years.size).foreach { j =>
          val intersection = (idsSeq(i) intersect idsSeq(j)).filter { id => yearsCounts(id) == 2 }
          pieDataset.setValue(s"$year & ${years(j)}", intersection.size)
        }
    }

    if (years.size > 3) {
      years.zipWithIndex.foreach {
        case (year, i) =>
          val withoutOne = removeByIndex(years, i)
          val key = withoutOne.mkString(" & ")
          pieDataset.setValue(key, yearsMap(key).size)
      }
    }

    if (years.size > 1) {
      pieDataset.setValue(years.mkString(" & "), intersectAll.size)
    }

    pieDataset
  }

  def removeByIndex[T](seq: Seq[T], i: Int): Seq[T] = seq.take(i) ++ seq.drop(i + 1)

  def saveCharts(chart: JFreeChart, name: String, width: Int, height: Int) {
    //charts.saveAsJPEG(chart, name + ".jpg", width, height)
    saveAsPNG(chart, name + ".png", width, height)
    //charts.saveAsSVG(chart, name + ".svg", width, height)
  }


}
