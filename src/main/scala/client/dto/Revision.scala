package client.dto

case class Revision(user:String, timestamp: String, comment: String, content: String) {

  def this(content: String) = this(null, null, null, content)

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
