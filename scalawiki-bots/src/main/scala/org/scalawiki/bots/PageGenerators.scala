package org.scalawiki.bots

import org.rogach.scallop.ScallopConf

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

class PageGeneratorsScallop(arguments: Seq[String]) extends ScallopConf(arguments) {
  val category = opt[String]("cat", descr = "Work on all pages which are in a specific category.", required = false)
  val namespace = opt[String]("namespace", descr = "Work on all pages in given namespaces.", required = false)
}
