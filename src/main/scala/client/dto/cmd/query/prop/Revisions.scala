package client.dto.cmd.query.prop

import client.dto.cmd.EnumArgument

object Revisions extends  EnumArgument[PropArg]("revisions", "Get revision information.") with PropArg
