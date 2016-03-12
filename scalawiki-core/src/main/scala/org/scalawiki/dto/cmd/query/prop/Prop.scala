package org.scalawiki.dto.cmd.query.prop

import org.scalawiki.dto.cmd.query.{GeneratorArg, QueryParam}
import org.scalawiki.dto.cmd.{EnumArg, EnumParameter}

/**
 *  ?action=query&amp;prop= argument
 *
 */

trait PropArg extends EnumArg[PropArg] with GeneratorArg { val param = Prop }

/**
 * ?action=query&amp;prop= parameter
 */
case class Prop(override val args: PropArg*) extends EnumParameter[PropArg]("prop", "") with QueryParam[PropArg]
