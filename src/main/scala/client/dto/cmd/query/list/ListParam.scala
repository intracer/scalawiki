package client.dto.cmd.query.list

import client.dto.cmd.{EnumArg, Action}

/**
 *  ?action=query&amp;list= parameter
 *
 */

object ListParam extends Action[ListArg]("list", "")

/**
 *  ?action=query&amp;list= argument
 *
 */
trait ListArg extends EnumArg[ListArg] { val param = ListParam }
