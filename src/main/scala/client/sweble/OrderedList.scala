package client.sweble

import org.sweble.wikitext.parser.nodes._
import scala.collection.JavaConverters._

object OrderedList {
    def toStringSeq(node: WtOrderedList): Seq[String] = {
      node.asScala.map(_.get(0).asInstanceOf[WtText].getContent.trim).toSeq
    }

    def definitionLists(nodes: TraversableOnce[WtNode]) = {
      nodes.toIterable.sliding(2).map(_.toSeq).flatMap {
        case Seq(defList: WtDefinitionList, orderedList: WtOrderedList) => Some(
          defList.get(0).asInstanceOf[WtDefinitionListTerm].get(0).asInstanceOf[WtText].getContent ->
            toStringSeq(orderedList)
        )
        case _ => None
      }
    }
}
