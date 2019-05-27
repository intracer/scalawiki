package org.scalawiki.wlx

import org.scalawiki.MwBot
import org.scalawiki.dto.markup.SwTemplate
import org.scalawiki.edit.{PageUpdateTask, PageUpdater}
import org.scalawiki.wikitext.SwebleParser
import org.scalawiki.wlx.dto.Monument
import org.sweble.wikitext.engine.config.WikiConfig
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp
import org.sweble.wikitext.parser.nodes.WtTemplate

object ListUpdater {

  def fillLists(monumentDb: MonumentDB, monumentUpdater: MonumentUpdater) {
    val task = new ListUpdaterTask(MwBot.ukWiki, monumentDb, monumentUpdater)
    val updater = new PageUpdater(task)
    updater.update()
  }
}

trait MonumentUpdater {
  def needsUpdate(m: Monument): Boolean
  def updatedParams(m: Monument): Map[String, String]
}

class ListUpdaterTask(val host: String, monumentDb: MonumentDB, monumentUpdater: MonumentUpdater) extends PageUpdateTask with SwebleParser {

  val config: WikiConfig = DefaultConfigEnWp.generate

  val titles = pagesToFill(monumentDb)

  val uploadConfig = monumentDb.contest.uploadConfigs.head

  override def updatePage(title: String, pageText: String): (String, String) = {
    val template = uploadConfig.listTemplate
    val wlxParser = new WlxTemplateParser(uploadConfig.listConfig, title)
    var updated: Int = 0

    def mapper(wtTemplate: WtTemplate) = {
      val swTemplate = new SwTemplate(wtTemplate)
      val monument = wlxParser.templateToMonument(swTemplate.template)

      if (needsUpdate(monument)) {
        updated += 1

        val updates = monumentUpdater.updatedParams(monument)

        updates.foreach { case (name, value) =>
          swTemplate.setTemplateParam(name, value)
        }
      }
    }

    val noComments = pageText
      .replace("<!--", "<x-comment>")
      .replace("-->", "</x-comment>")

    val newText = replace(noComments, { case t: WtTemplate if getTemplateName(t) == template => t }, mapper)

    val withComments = newText
      .replace("<x-comment>", "<!--")
      .replace("</x-comment>", "-->")

    val comment = s"updated $updated monument(s)"
    (withComments, comment)
  }

  def needsUpdate(m: Monument): Boolean = monumentUpdater.needsUpdate(m)

  def pagesToFill(monumentDb: MonumentDB): Set[String] = {

    val monumentsToFill = monumentDb.monuments.filter(needsUpdate)

    println(s"NewIds: ${monumentsToFill.size}")

    val titles = monumentsToFill.map(_.page).toSet

    println(s"pages: ${titles.size}")
    titles
  }
}

