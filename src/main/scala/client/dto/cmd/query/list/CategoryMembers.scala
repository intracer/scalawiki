package client.dto.cmd.query.list

import client.dto.cmd.{IntListParameter, StringListParameter, Parameter, ArgWithParams}
import client.dto.cmd.query.Module

case class CategoryMembers(override val params: CmParam[Any]*)
  extends Module[ListArg]("cm", "categorymembers", "List all pages in a given category.")
  with ListArg
  with ArgWithParams[CmParam[Any], ListArg]

trait CmParam[+T] extends Parameter[T]

object CmTitle extends StringListParameter("cmtitle", "Title to search. Cannot be used together with cmpageid.") with CmParam[String]
object CmPageId extends IntListParameter("cmpageid", "Page ID to search. Cannot be used together with cmtitle.") with CmParam[Int]
object CmLimit extends StringListParameter("cmlimit", "How many total pages to return.") with CmParam[String]

//cmtitle Which category to enumerate (required).
//cmpageid Page ID of the category to enumerate. Cannot be used together with cmtitle.
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
