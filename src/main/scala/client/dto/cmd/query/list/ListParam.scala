package client.dto.cmd.query.list

import client.dto.cmd.{EnumArg, EnumParameter}

/**
 *  ?action=query&amp;list= parameter
 *
 */

object ListParam extends EnumParameter[ListArg]("list", "")

/**
 *  ?action=query&amp;list=argument
 *
 */
trait ListArg extends EnumArg[ListArg] { val param = ListParam }
