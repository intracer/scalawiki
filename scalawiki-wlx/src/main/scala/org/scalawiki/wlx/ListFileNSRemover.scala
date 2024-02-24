package org.scalawiki.wlx

import org.scalawiki.MwBot
import org.scalawiki.dto.markup.SwTemplate
import org.scalawiki.edit.{PageUpdateTask, PageUpdater}
import org.scalawiki.wikitext.SwebleParser
import org.scalawiki.wlx.dto.Monument
import org.sweble.wikitext.engine.config.WikiConfig
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp
import org.sweble.wikitext.parser.nodes.WtTemplate

object ListFileNSRemover {

  def updateLists(monumentDb: MonumentDB) {
    val task = new ListFileNSRemoverTask(MwBot.ukWiki, monumentDb)
    val updater = new PageUpdater(task)
    updater.update()
  }
}

class ListFileNSRemoverTask(val host: String, monumentDb: MonumentDB)
    extends PageUpdateTask
    with SwebleParser {

  val config: WikiConfig = DefaultConfigEnWp.generate

  val titles = pagesToFix(monumentDb)

  val uploadConfig = monumentDb.contest.uploadConfigs.head

  override def updatePage(title: String, pageText: String): (String, String) = {
    val template = uploadConfig.listTemplate
    val wlxParser = new WlxTemplateParser(uploadConfig.listConfig, title)
    var added: Int = 0

    def mapper(wtTemplate: WtTemplate) = {
      val swTemplate = new SwTemplate(wtTemplate)
      val monument = wlxParser.templateToMonument(swTemplate.template)

      if (needsUpdate(monument)) {
        added += 1
        val value =
          monument.photo.get.replaceFirst("File:", "").replaceFirst("Файл:", "")
        val name = wlxParser.image.get
        swTemplate.setTemplateParam(name, value)
      }
    }

    val newText = replace(
      pageText,
      { case t: WtTemplate if getTemplateName(t) == template => t },
      mapper
    )
    val comment = s"fixing $added image(s)"
    (newText, comment)
  }

  def needsUpdate(m: Monument): Boolean =
    m.photo.exists(photo =>
      photo.trim.startsWith("File:") || photo.trim.startsWith("Файл:")
    )

  def pagesToFix(monumentDb: MonumentDB): Set[String] = {

    val monumentsToFill = monumentDb.monuments.filter(needsUpdate)

    println(s"Ids: ${monumentsToFill.size}")

    val titles = monumentsToFill.map(_.page).toSet

    println(s"pages: ${titles.size}")
    titles
  }

}
