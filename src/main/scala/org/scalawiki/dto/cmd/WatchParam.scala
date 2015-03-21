package org.scalawiki.dto.cmd

trait WatchParam[+T] extends Parameter[T]

case class Watch(override val arg: Boolean = true) extends BooleanParameter("watch",
  "Add the page to your watchlist. Deprecated. Use the watchlist argument") with WatchParam[Boolean]

case class UnWatch(override val arg: Boolean = true) extends BooleanParameter("unwatch",
  "Remove the page from your watchlist. Deprecated. Use the watchlist argument") with WatchParam[Boolean]

// TODO single Enum value arg
case class WatchList(override val args: WatchListArg*) extends EnumParameter[WatchListArg]("watchlist",
  "Specify how the watchlist is affected by this edit") with WatchParam[WatchListArg]



trait WatchListArg extends EnumArg[WatchListArg] { val param = WatchList }

object WLWatch extends EnumArgument[WatchListArg]("watch", "add the page to the watchlist.") with WatchListArg
object WLUnWatch extends EnumArgument[WatchListArg]("unwatch", "remove the page from the watchlist.") with WatchListArg
object WLPreferences extends EnumArgument[WatchListArg]("preferences", "use the preference settings (Default).") with WatchListArg
object WLNoChange extends EnumArgument[WatchListArg]("nochange", "don't change the watchlist.") with WatchListArg