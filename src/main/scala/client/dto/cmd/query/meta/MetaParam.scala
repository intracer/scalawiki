package client.dto.cmd.query.meta

import client.dto.cmd.{EnumArg, Action}

object MetaParam extends Action[MetaArg]("meta", "")
trait MetaArg extends EnumArg[MetaArg] { val param = MetaParam }
