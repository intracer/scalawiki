package client.dto.cmd.query.prop

import client.dto.cmd._

/**
 *  ?action=query&amp;prop=info
 *
 */
object Info extends EnumArgument[PropArg]("info", "Get basic page information.") with PropArg with ArgWithParams[InfoParam, PropArg]


/**
 * Marker trait for parameters used with prop=info
 */
trait InfoParam extends Parameter[AnyRef]


/**
 *  ?action=query&amp;prop=info&amp;inprop=
 *
 */
object InProp extends EnumParameter[InPropArg]("inprop", "") with InfoParam


/**
 *  Trait for inprop= arguments
 *
 */

trait InPropArg extends EnumArg[InPropArg] { val param = InProp }

/**
 *  inprop= arguments
 *
 */
object Protection extends EnumArgument[InPropArg]("protection", "List the protection level of each page.") with InPropArg
object TalkId extends EnumArgument[InPropArg]("talkid", "The page ID of the talk page for each non-talk page.") with InPropArg
object Watched extends EnumArgument[InPropArg]("watched", "List the watched status of each page.") with InPropArg
object Watchers extends EnumArgument[InPropArg]("watchers", "The page ID of the talk page for each non-talk page.") with InPropArg
object NotificationTimestamp extends EnumArgument[InPropArg]("notificationtimestamp", "The watchlist notification timestamp of each page.") with InPropArg
object SubjectId extends EnumArgument[InPropArg]("subjectid", "The page ID of the parent page for each talk page.") with InPropArg
object Url extends EnumArgument[InPropArg]("url", "Gives a full URL, an edit URL, and the canonical URL for each page.") with InPropArg
object Readable extends EnumArgument[InPropArg]("readable", "Whether the user can read this page.") with InPropArg
object Preload extends EnumArgument[InPropArg]("preload", "Gives the text returned by EditFormPreloadText.") with InPropArg
object DisplayTitle extends EnumArgument[InPropArg]("displaytitle", "Gives the way the page title is actually displayed.") with InPropArg
