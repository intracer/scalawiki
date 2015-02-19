package client.dto.cmd.query.list

import client.dto.cmd.query.QueryParam
import client.dto.cmd.{EnumArg, EnumParameter}

/**
 *  ?action=query&amp;list= parameter
 *
 */

case class ListParam(override val args: ListArg*) extends EnumParameter[ListArg]("list", "") with QueryParam[ListArg]

/**
 *  ?action=query&amp;list=argument
 *
 */
trait ListArg extends EnumArg[ListArg] { val param = ListParam }
