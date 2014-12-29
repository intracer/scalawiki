package client.sweble

import org.sweble.wikitext.parser.nodes.{WtNode, WtText, WtSection}

class Section(val wtSection: WtSection) {
  def heading =
    wtSection.getHeading.get(0).asInstanceOf[WtText].getContent.trim
}

object Section {
  def apply(wtNode: WtNode) = wtNode match  {
    case wtSection: WtSection => Some(new Section(wtSection))
    case _ => None 
  }
  
  def fromTraversable(nodes: TraversableOnce[WtNode]) = nodes.flatMap(Section.apply)
  
  def isSection(wtNode: WtNode) = wtNode.getNodeType == WtNode.NT_SECTION
}