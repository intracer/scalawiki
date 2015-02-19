package client.dto.cmd.query.meta

import client.dto.cmd.query.QueryParam
import client.dto.cmd.{EnumArg, EnumParameter}

case class MetaParam(override val args: MetaArg*) extends EnumParameter[MetaArg]("meta", "") with QueryParam[MetaArg]
trait MetaArg extends EnumArg[MetaArg] { val param = MetaParam }
