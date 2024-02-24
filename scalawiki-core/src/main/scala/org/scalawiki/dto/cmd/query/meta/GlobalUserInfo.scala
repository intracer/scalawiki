package org.scalawiki.dto.cmd.query.meta

import org.scalawiki.dto.cmd._
import org.scalawiki.dto.cmd.query.Module

/** ?action=query&amp;meta=globaluserinfo
  */
case class GlobalUserInfo(override val params: GuiParam[Any]*)
    extends Module[MetaArg](
      "gui",
      "globaluserinfo",
      "Show information about a global user."
    )
    with MetaArg
    with ArgWithParams[GuiParam[Any], MetaArg]

trait GuiParam[+T] extends Parameter[T]

case class GuiUser(override val arg: String)
    extends StringParameter(
      "guiuser",
      "User to get information about. Defaults to the current user."
    )
    with GuiParam[String]

/** Which properties to get: ?action=query&amp;meta=globaluserinfo&amp;guiprop=
  */
case class GuiProp(override val args: GuiPropArg*)
    extends EnumParameter[GuiPropArg]("guiprop", "Which properties to get.")
    with GuiParam[GuiPropArg]

/** Trait for guiprop= arguments
  */

trait GuiPropArg extends EnumArg[GuiPropArg] {
  val param = GuiProp
}

/** guiprop= arguments
  */

object Groups
    extends EnumArgument[GuiPropArg](
      "groups",
      "Get a list of global groups this user belongs to."
    )
    with GuiPropArg

object Rights
    extends EnumArgument[GuiPropArg](
      "rights",
      "Get a list of global rights this user has."
    )
    with GuiPropArg

object Merged
    extends EnumArgument[GuiPropArg]("merged", "Get a list of merged accounts.")
    with GuiPropArg

object Unattached
    extends EnumArgument[GuiPropArg](
      "unattached",
      "Get a list of unattached accounts."
    )
    with GuiPropArg

object EditCount
    extends EnumArgument[GuiPropArg](
      "editcount",
      "Get the user's global editcount."
    )
    with GuiPropArg
