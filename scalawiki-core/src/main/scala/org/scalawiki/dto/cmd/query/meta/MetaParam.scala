package org.scalawiki.dto.cmd.query.meta

import org.scalawiki.dto.cmd.{EnumArg, EnumParameter}
import org.scalawiki.dto.cmd.query.QueryParam

case class MetaParam(override val args: MetaArg*)
    extends EnumParameter[MetaArg]("meta", "")
    with QueryParam[MetaArg]
trait MetaArg extends EnumArg[MetaArg] { val param = MetaParam }
