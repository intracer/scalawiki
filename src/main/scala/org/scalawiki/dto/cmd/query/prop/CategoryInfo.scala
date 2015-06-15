package org.scalawiki.dto.cmd.query.prop

import org.scalawiki.dto.cmd.query.Module

/**
 *  ?action=query&amp;prop=langlinks
 *
 */
object CategoryInfo
  extends Module[PropArg]("ci", "categoryinfo", "Gets information about categories.")
  with PropArg


