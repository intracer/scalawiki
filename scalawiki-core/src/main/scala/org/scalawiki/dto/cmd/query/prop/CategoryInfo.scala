package org.scalawiki.dto.cmd.query.prop

import org.scalawiki.dto.cmd.query.Module

/** ?action=query&amp;prop=categoryinfo
  */
object CategoryInfo
    extends Module[PropArg](
      "ci",
      "categoryinfo",
      "Gets information about categories."
    )
    with PropArg
