package org.scalawiki.wlx.stat

import org.scalawiki.MwBot
import org.scalawiki.cache.CachedBot
import org.scalawiki.dto.cmd.Action
import org.scalawiki.dto.cmd.query.prop.iiprop.{IiProp, Url}
import org.scalawiki.dto.cmd.query.prop.rvprop.RvProp
import org.scalawiki.dto.cmd.query.prop.{ImageInfo, Prop, Revisions, rvprop}
import org.scalawiki.dto.cmd.query.{PageIdsParam, Query}
import org.scalawiki.dto.{Image, Namespace, Page, Revision, Site}
import org.scalawiki.query.QueryLibrary
import org.scalawiki.wlx.ImageDB

import scala.concurrent.ExecutionContext.Implicits.global

object NominatedForDeletion extends QueryLibrary {
  val commons = MwBot.fromHost(MwBot.commons)
  val ukWiki = MwBot.fromHost(MwBot.ukWiki)

  def report(stat: ContestStat) = {
    val findNominatedAction = Action(Query(generatorWithTemplate("Delete", Set(Namespace.FILE))))

    val cachedBot = new CachedBot(Site.commons, "delete", true)
    cachedBot.run(findNominatedAction).map { nominated =>
      println("all nominated:" + nominated.size)

      stat.totalImageDb.map { db =>
        val wlmIds = db.images.flatMap(_.pageId).toSet
        val nominatedIds = nominated.flatMap(_.id).toSet
        val wlmNominatedIds = wlmIds.intersect(nominatedIds)
        println("wlm nominated:" + wlmNominatedIds.size)

        makeGallery(db, wlmNominatedIds)

        //        copyToWikipedia(wlmNominatedIds)
      }
    }
  }

  private def makeGallery(db: ImageDB, wlmNominatedIds: Set[Long]) = {
    val images = db.images.filter(_.pageId.exists(wlmNominatedIds.contains)).map(_.title).sorted
    val gallery = Image.gallery(images)
    commons.page("Commons:Wiki Loves Monuments in Ukraine nominated for deletion")
      .edit(gallery)
  }

  private def copyToWikipedia(wlmNominatedIds: Set[Long]) = {
    val nominatedInfoAction = Action(Query(
      PageIdsParam(wlmNominatedIds.toSeq),
      Prop(
        Revisions(Seq(RvProp(rvprop.Ids, rvprop.Content)): _*),
        ImageInfo(IiProp(Url))
      )))

    commons.run(nominatedInfoAction).map { nominatedInfos =>
      nominatedInfos.map { info =>
        copyImage(info)
      }
    }
  }

  private def copyImage(page: Page) = {
    val image = page.images.head
    for (text <- page.text;
         url <- image.url) {
      commons.getByteArray(url).map { bytes =>
        val title = page.title
        ukWiki.page(title).upload(title, bytes, page.text, Some("moving from commons"))
      }
    }
  }

  def main(args: Array[String]): Unit = {
    val url = "https://upload.wikimedia.org/wikipedia/commons/c/c3/10._%D0%A2%D0%B5%D1%80%D0%BD%D0%BE%D0%BF%D1%96%D0%BB%D1%8C_%D0%9F%D0%B0%D0%BC%27%D1%8F%D1%82%D0%BD%D0%B8%D0%BA_%D0%BF%D0%BE%D0%B5%D1%82%D1%83%2C_%D0%BF%D0%B8%D1%81%D1%8C%D0%BC%D0%B5%D0%BD%D0%BD%D0%B8%D0%BA%D1%83_%D0%9F%D1%83%D1%88%D0%BA%D1%96%D0%BD_%D0%9E%D0%BB%D0%B5%D0%BA%D1%81%D0%B0%D0%BD%D0%B4%D1%80.JPG"
    val title = "File:10. Тернопіль Пам'ятник поету, письменнику Пушкін Олександр.JPG"
    val text = """{{delete|reason=There is [[Commons:Freedom of panorama#Ukraine|no freedom of panorama in Ukraine]] and the photos violate sculptors' and architects' copyright. Created 1959. No Permission from the sculptors Макар Вронський, Олексій Олійник, Олександр Скобліков.|subpage=File:Тернопіль, вул. Чорновола (сквер), Пам'ятник поету, письменнику Олександрові Пушкіну.jpg|year=2020|month=July|day=31}}
                 |=={{int:filedesc}}==
                 |{{Information
                 ||description={{uk|1=Пам'ятник поету, письменнику [[:uk:Пушкін Олександр Сергійович|Олександрові Пушкіну]], [[:uk:Тернопіль|Тернопіль]], [[:uk:Вулиця В'ячеслава Чорновола (Тернопіль)|вул. Чорновола]] (сквер)}}{{Monument Ukraine|61-101-0237}}
                 ||date=2015-05-03 07:10:52
                 ||source={{own}}
                 ||author=[[User:Neovitaha777|Neovitaha777]]
                 ||permission=
                 ||other versions=
                 |}}
                 |{{Location dec|49.5532373|25.5958934}}
                 |
                 |=={{int:license-header}}==
                 |{{self|cc-by-sa-4.0}}
                 |
                 |
                 |{{Wiki Loves Monuments 2015|ua}}
                 |[[Category:Statue of Oleksandr Pushkin in Ternopil]]
                 |
                 |[[Category:Uploaded via Campaign:wlm-ua]]
                 |[[Category:Ukraine photographs taken on 2015-05-03]]
                 |""".stripMargin
    val page = new Page(
      id = None, ns = Some(Namespace.FILE), title = title, revisions = Seq(Revision(content = Some(text))),
      images = Seq(Image(title, url = Some(url)))
    )
    copyImage(page)
  }
}
