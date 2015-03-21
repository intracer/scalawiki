package org.scalawiki.dto.cmd.query.prop

import org.scalawiki.dto.cmd.query.QueryParam
import org.scalawiki.dto.cmd.{EnumArg, EnumParameter}

/**
 *  ?action=query&amp;prop= argument
 *
 */

trait PropArg extends EnumArg[PropArg] { val param = Prop }

/**
 * ?action=query&amp;prop= parameter
 */
case class Prop(override val args: PropArg*) extends EnumParameter[PropArg]("prop", "") with QueryParam[PropArg]
