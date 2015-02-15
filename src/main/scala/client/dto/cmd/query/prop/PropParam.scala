package client.dto.cmd.query.prop

import client.dto.cmd.{EnumArg, EnumParameter}

trait PropArg extends EnumArg[PropArg] { val param = PropParam }

object PropParam extends EnumParameter[PropArg]("prop", "")
