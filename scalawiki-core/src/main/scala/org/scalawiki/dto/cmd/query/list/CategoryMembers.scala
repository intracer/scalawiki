package org.scalawiki.dto.cmd.query.list

import org.scalawiki.dto.cmd._
import org.scalawiki.dto.cmd.query.Module
import org.scalawiki.dto.cmd.query.prop.rvprop.RvProp

case class CategoryMembers(override val params: CmParam[Any]*)
  extends Module[ListArg]("cm", "categorymembers", "List all pages in a given category.")
  with ListArg
  with ArgWithParams[CmParam[Any], ListArg] {

  def this(title: Option[String], pageId: Option[Long], namespaces: Set[Int], limit: Option[String], params: CmParam[Any]*) = {
    this(Seq(
      title.map(CmTitle).toSeq,
      pageId.map(CmPageId).toSeq,
      limit.map(CmLimit).toSeq
    ).flatten ++
      (if (namespaces.nonEmpty)
        Seq(CmNamespace(namespaces.toSeq))
      else Seq.empty)
      ++ params: _*)
  }
}

trait CmParam[+T] extends Parameter[T]

trait CmTypeArg extends EnumArg[CmTypeArg] {
  val param = CmType
}


case class CmTitle(override val arg: String) extends StringParameter("cmtitle",
  "Title to search. Cannot be used together with cmpageid.") with CmParam[String]

case class CmPageId(override val arg: Long) extends IdParameter("cmpageid",
  "Page ID to search. Cannot be used together with cmtitle.") with CmParam[Long]

case class CmNamespace(override val args: Seq[Int]) extends IntListParameter("cmnamespace",
  "Only include pages in these namespaces.") with CmParam[Int]

// TODO Seq[Int] ?

case class CmLimit(override val arg: String) extends StringParameter("cmlimit",
  "How many total pages to return.") with CmParam[String]

case class CmType(override val args: CmTypeArg*)
  extends EnumParameter[CmTypeArg]("cmtype", "Which type of category members to include") with CmParam[CmTypeArg]

object CmTypePage extends EnumArgument[CmTypeArg]("page", "pages") with CmTypeArg
object CmTypeSubCat extends EnumArgument[CmTypeArg]("subcat", "subcats") with CmTypeArg
object CmTypeFile extends EnumArgument[CmTypeArg]("file", "files") with CmTypeArg


//cmprop
//Which pieces of information to include:
//ids
//Adds the page ID.
//title
//Adds the title and namespace ID of the page.
//sortkey
//Adds the sortkey used for sorting in the category (hexadecimal string).
//sortkeyprefix
//Adds the sortkey prefix used for sorting in the category (human-readable part of the sortkey).
//type
//Adds the type that the page has been categorised as (page, subcat or file).
//timestamp
//Adds the timestamp of when the page was included.
//cmnamespace
//Only include pages in these namespaces. Note that cmtype=subcat or cmtype=file may be used instead of cmnamespace=14 or 6.
//Note: Due to miser mode, using this may result in fewer than cmlimit results returned before continuing; in extreme cases, zero results may be returned.
//cmtype
//Which type of category members to include. Ignored when cmsort=timestamp is set.
//cmcontinue
//When more results are available, use this to continue.
//cmlimit
//The maximum number of pages to return.
//cmsort
//Property to sort by.
//cmdir
//In which direction to sort.
//cmstart
//Timestamp to start listing from. Can only be used with cmsort=timestamp.
//cmend
//Timestamp to end listing at. Can only be used with cmsort=timestamp.
//cmstarthexsortkey
//Sortkey to start listing from, as returned by cmprop=sortkey. Can only be used with cmsort=sortkey.
//cmendhexsortkey
//Sortkey to end listing from, as returned by cmprop=sortkey. Can only be used with cmsort=sortkey.
//cmstartsortkeyprefix
//Sortkey prefix to start listing from. Can only be used with cmsort=sortkey. Overrides cmstarthexsortkey.
//cmendsortkeyprefix
//Sortkey prefix to end listing BEFORE (not at, if this value occurs it will not be included!). Can only be used with cmsort=sortkey. Overrides cmendhexsortkey.
//cmstartsortkey
//Use cmstarthexsortkey instead.
//(застаріло)
//cmendsortkey
//Use cmendhexsortkey instead.
//(застаріло)
