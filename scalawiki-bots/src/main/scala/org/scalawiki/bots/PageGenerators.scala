package org.scalawiki.bots

import com.concurrentthought.cla.{Args, Opt}


case class PageGenConfig(cat: Seq[String] = Seq.empty, //,
                         //                         catR: String,
                         //                         subCats: String,
                         //                         subCatsR: String,
                         //                         unCat: String,
                         //                         unCatCat: String,
                         //                         unCatFiles: String,
                         //                         file: String,
                         //                         fileLinks: String,
                         //                         search: String,
                         //                         logEvents: String,
                                                  namespaces: Seq[String] = Seq.empty
                         //                         interwiki: String,
                         //                         limit: Int,
                         //                         links: String,
                         //                         liveRecentChanges: Option[Int],
                         //                         imagesUsed: String,
                         //                         newImages: Option[Int],
                         //                         newPages: Option[Int],
                         //                         recentChanges: String,
                         //                         unconnectedPages: Option[Int],
                         //                         ref: String,
                         //                         start: String,
                         //                         prefixIndex: String,
                         //                         subPage: Int,
                         //                         titleRegex: String,
                         //                         transcludes: String,
                         //                         unusedFiles: Option[Int],
                         //                         lonelyPages: Option[Int],
                         //                         unwatched: Option[Int],
                         //                         userContribs: String,
                         //                         weblink: String,
                         //                         withoutInterwiki: Option[Int],
                         //                         sqlQuery: String,
                         //                         wikidataQuery: String,
                         //                         sparqlQuery: String,
                         //                         random: Option[Int],
                         //                         randomRedirect: Option[Int],
                         //                         page: Seq[String],
                         //                         pageId : Seq[Long],
                         //                         grep: String,
                         //                         quality: Int,
                         //                         onlyIf: String,
                         //                         onlyIfNot: String
                        )

object PageGenerators {

  val category = Opt.seqString(delimsRE = "[,|]")(
    name = "category",
    flags = Seq("-cat"),
    help = "Work on all pages which are in a specific category."
  )

  val namespace = Opt.seqString(delimsRE = "[,|]")(
    name = "namespace",
    flags = Seq("-ns", "-namespace", "-namespaces"),
    help = "Work on all pages in given namespaces."
  )

  val opts = Seq(category, namespace)

  def argsToConfig(args: Args): PageGenConfig = {
    def nameToSeq(name: String) = args.values(name).asInstanceOf[Seq[String]]
    PageGenConfig(
      cat = nameToSeq("category"),
      namespaces = nameToSeq("namespace")
    )
  }

}
