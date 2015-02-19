package client.dto

import client.dto.Page.Id

case class Revision(
                     revId: Option[Id] = None,
                     parentId: Option[Id] = None,
                     user: Option[String] = None,
                     userId: Option[Id] = None,
                     timestamp: Option[String] = None,
                     comment: Option[String] = None,
                     content: Option[String] = None,
                     size: Option[Int] = None) {

  def this(content: String) = this(content = Some(content))

}


//"revisions": [
//{
//"user": "77.56.53.183",
//"anon": "",
//"timestamp": "2014-05-18T01:18:42Z",
//"comment": "/* Awards and recognition */",
//"contentformat": "text/x-wiki",
//"contentmodel": "wikitext",
//"*"
