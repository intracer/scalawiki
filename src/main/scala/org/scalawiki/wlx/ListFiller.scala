package org.scalawiki.wlx

import org.scalawiki.MwBot
import org.scalawiki.dto.markup.SwTemplate
import org.scalawiki.edit.{PageUpdateTask, PageUpdater}
import org.scalawiki.wikitext.SwebleParser
import org.scalawiki.wlx.dto.{Image, Monument}
import org.sweble.wikitext.engine.config.WikiConfig
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp
import org.sweble.wikitext.parser.nodes.WtTemplate

object ListFiller {

  def fillLists(monumentDb: MonumentDB, imageDb: ImageDB) {
    val task = new ListFillerTask(MwBot.ukWiki, monumentDb, imageDb)
    val updater = new PageUpdater(task)
    updater.update()
  }

  def bestImage(images: Seq[Image]) =
    images.sortBy(image => image.size.get + image.width.get * image.height.get).last

}

class ListFillerTask(val host: String, monumentDb: MonumentDB, imageDb: ImageDB) extends PageUpdateTask with SwebleParser {

  val config: WikiConfig = DefaultConfigEnWp.generate

  val titles = pagesToFill(monumentDb, imageDb)

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
        val image = ListFiller.bestImage(imageDb.byId(monument.id))
        val name = wlxParser.image.get
        val value = image.title.replaceFirst("File:", "").replaceFirst("Файл:", "")
        swTemplate.setTemplateParam(name, value)
      }
    }

    val newText = replace(pageText, { case t: WtTemplate if getTemplateName(t) == template => t }, mapper)
    val comment = s"adding $added image(s)"
    (newText, comment)
  }

  def needsUpdate(m: Monument): Boolean =
    m.photo.isEmpty && imageDb.containsId(m.id)

  def pagesToFill(monumentDb: MonumentDB, imageDb: ImageDB): Set[String] = {

    val monumentsToFill = monumentDb.monuments.filter(needsUpdate)

    println(s"NewIds: ${monumentsToFill.size}")

    val titles = monumentsToFill.map(_.page).toSet

    println(s"pages: ${titles.size}")
    titles
  }

}

