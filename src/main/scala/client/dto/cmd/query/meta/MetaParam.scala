package client.dto.cmd.query.meta

import client.dto.cmd.{EnumArg, EnumParameter}

object MetaParam extends EnumParameter[MetaArg]("meta", "")
trait MetaArg extends EnumArg[MetaArg] { val param = MetaParam }
