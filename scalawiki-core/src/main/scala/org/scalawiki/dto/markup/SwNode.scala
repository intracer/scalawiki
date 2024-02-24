package org.scalawiki.dto.markup

import org.sweble.wikitext.parser.nodes.WtNode
import org.sweble.wikitext.parser.utils.WtRtDataPrinter

trait SwNode {

  def wtNode: WtNode

  def getText: String = getText(wtNode)

  def getText(wtNodeP: WtNode): String = WtRtDataPrinter.print(wtNodeP)

}
