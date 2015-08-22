package org.scalawiki.dto.cmd.query.list

import org.scalawiki.dto.cmd.{EnumArg, EnumParameter}
import org.scalawiki.dto.cmd.query.{GeneratorArg, QueryParam}

/**
 *  ?action=query&amp;list= parameter
 *
 */

case class ListParam(override val args: ListArg*) extends EnumParameter[ListArg]("list", "") with QueryParam[ListArg]

/**
 *  ?action=query&amp;list=argument
 *
 */
trait ListArg extends EnumArg[ListArg] with GeneratorArg { val param = ListParam }
