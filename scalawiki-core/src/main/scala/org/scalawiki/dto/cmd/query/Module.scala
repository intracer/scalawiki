package org.scalawiki.dto.cmd.query

import org.scalawiki.dto.cmd.{EnumArg, EnumArgument}

class Module[T <: EnumArg[T]](val prefix: String, name: String, summary: String)
    extends EnumArgument[T](name, summary) {}
