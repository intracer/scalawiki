package org.scalawiki.wlx

import java.awt.{Color, Font, Rectangle}
import java.io.File
import java.text.DecimalFormat

import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.labels.StandardPieSectionLabelGenerator
import org.jfree.chart.plot.{PiePlot, PlotOrientation}
import org.jfree.chart.renderer.category.{BarRenderer, StandardBarPainter}
import org.jfree.chart.{ChartFactory, ChartUtilities, JFreeChart}
import org.jfree.data.category.{CategoryDataset, DefaultCategoryDataset}
import org.jfree.data.general.{DefaultPieDataset, PieDataset}
import org.jfree.graphics2d.svg.{SVGGraphics2D, SVGUtils}
import org.scalawiki.{MwBot, WithBot}

class Charts extends WithBot {

  override def host: String = MwBot.commons

  val color2014 = new Color(220, 57, 18)
  val color2013 = new Color(255, 153, 0)
  val color2012 = new Color(51, 102, 204)

  def init {
    val dataset = createTotalDataset(Seq(2001, 2002, 2003),  Seq(1, 2, 3))
    val chart = createChart(dataset, "")

    saveAsPNG(createPieChart(createPieDataset(), "Унікальність фотографій пам'яток за роками"), "WikiLovesMonumentsInUkrainePicturedByYearPieTest.png", 900, 900)
  }

  def saveAsJPEG(chart: JFreeChart, filename: String, width: Int, height: Int) {
    ChartUtilities.saveChartAsJPEG(new File(filename), chart, width, height)
  }

  def saveAsPNG(chart: JFreeChart, filename: String, width: Int, height: Int) {
    ChartUtilities.saveChartAsPNG(new File(filename), chart, width, height)
  }

  def saveAsSVG(chart: JFreeChart, filename: String, width: Int, height: Int) {
    val g2 = new SVGGraphics2D(width, height)
    chart.draw(g2, new Rectangle(width, height))

    SVGUtils.writeToSVG(new File(filename), g2.getSVGElement)
  }

  def createPieDataset() = {
    val dataset = new DefaultPieDataset()
    dataset.setValue("2012", 3240.0)
    dataset.setValue("2013", 3046.0)
    dataset.setValue("2014", 5328.0)
    dataset.setValue("2013 & 2014", 2274.0)
    dataset.setValue("2012 & 2014", 1069.0)
    dataset.setValue("2012 & 2013 & 2014", 2402.0)
    dataset.setValue("2012 & 2013", 1615.0)

    dataset
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

    plot.setSectionPaint("2012", color2012)
    plot.setSectionPaint("2012 & 2013", blend(color2012, color2013))
    plot.setSectionPaint("2013", color2013)
    plot.setSectionPaint("2013 & 2014", blend(color2013, color2014))
    plot.setSectionPaint("2014", color2014)
    plot.setSectionPaint("2012 & 2014", blend(color2012, color2014))
    plot.setSectionPaint("2012 & 2013 & 2014", new Color(0x99CC00))

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

    renderer.setSeriesPaint(0, color2014)
    renderer.setSeriesPaint(1, color2013)
    renderer.setSeriesPaint(2, color2012)

    //    val domainAxis = plot.getDomainAxis
    //    domainAxis.setCategoryLabelPositions(
    //      CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0)
    //    )

    chart
  }

}

object Charts {
  def main(args: Array[String]) {
    new Charts().init
  }

}
