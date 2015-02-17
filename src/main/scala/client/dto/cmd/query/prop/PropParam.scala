package client.dto.cmd.query.prop

import client.dto.cmd.query.QueryParam
import client.dto.cmd.{EnumArg, Action}

/**
 *  ?action=query&amp;prop= argument
 *
 */

trait PropArg extends EnumArg[PropArg] { val param = PropParam }

/**
 * ?action=query&amp;prop= parameter
 */
object PropParam extends Action[PropArg]("prop", "") with QueryParam[PropArg]
