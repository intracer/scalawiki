package client.dto.cmd.query

import client.dto.cmd.{EnumArg, EnumArgument}

class Module[T <: EnumArg[T]](val prefix: String, name: String, summary: String ) extends EnumArgument[T](name, summary)
{


}
