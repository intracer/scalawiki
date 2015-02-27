package client.xml

import java.io.IOException

class TangoParser(val content: Array[Char], val stripWhite: Boolean) {
  type XmlTokenType = Int

  var depth: Int = _
  var prefix: Array[Char] = _
  var rawValue: Array[Char] = _
  var localName: Array[Char] = _
  var typ: Int = XmlTokenType.None

  var text: XmlText = new XmlText()
  text.reset(content)

  var point: Int = 0
  var stream: Boolean = _
  var errMsg: String = _

  val retainWhite = !stripWhite
  val partialWhite = !stripWhite

  def end = text.size

  def next(): XmlTokenType = {
    var e = text.size
    var p = point

    // at end of document?
    if (p >= e)
      return endOfInput()
    if (stripWhite) {
      // strip leading whitespace
      while (text(p) <= 32) {
        p += 1
        if (p >= e)
          return endOfInput()
      }
    }
    // StartElement or Attribute?
    if (typ < XmlTokenType.EndElement) {
      if (retainWhite) {
        // strip leading whitespace (thanks to DRK)
        while (text(p) <= 32) {
          p += 1
          if (p >= e)
            return endOfInput()
        }
      }
      text(p) match {
        case '>' =>
          // termination of StartElement
          depth += 1
          p += 1

        case '/' =>

          point = p
          return doEndEmptyElement()

        case _ =>
          // must be attribute instead
          point = p
          return doAttributeName()
      }
    }

    // consume data between elements?
    if (text(p) != '<') {
      var q = p
      p += 1
      while (p < e && text(p) != '<') {
        p += 1
      }

      if (p < e) {
        if (partialWhite) {
          // include leading whitespace
          while (text(q - 1) <= 32)
            q -= 1
        }
        point = p
        rawValue = text.slice(q, p - q)
        typ = XmlTokenType.Data
        return typ
      }
      else
        return endOfInput()
    }

    // must be a '<' character, so peek ahead
    text(p + 1) match {
      case '!' =>
        // one of the following ...
        if (text.slice(p + 2, p + 4) sameElements "--".toCharArray) {
          point = p + 4
          return doComment()
        }
        else
        if (text.slice(p + 2, p + 9) sameElements "[CDATA[".toCharArray) {
          point = p + 9
          return doCData()
        }
        else
        if (text.slice(p + 2, p + 9) sameElements "DOCTYPE".toCharArray) {
          point = p + 9
          return doDoctype()
        }
        doUnexpected("!", p)

      case '?' =>
        // must be PI data
        point = p + 2
        doPI()

      case '/' =>
        // should be a closing element name
        p += 2
        var q = p
        while (text(q) > 63 || text.name(text(q)) > 0)
          q += 1
        if (text(q) == ':') {
          prefix = text.slice(p, q)
          q += 1
          p = q
          while (text(q) > 63 || text.attributeName(text(q)) > 0)
            q += 1

          localName = text.slice(p, q)
        }
        else {
          prefix = null
          localName = text.slice(p, q)
        }

        while (text(q) <= 32) {
          q += 1
          if (q >= e)
            return endOfInput()
        }

        if (text(q) == '>') {
          depth -= 1
          point = q + 1
          typ = XmlTokenType.EndElement
          return typ
        }
        doExpected(">", q)

      case _ =>
        // scan new element name
        p += 1
        var q = p
        while (text(q) > 63 || text.name(text(q)) > 0)
          q += 1

        // check if we ran past the end
        if (q >= e)
          return endOfInput()

        if (text(q) != ':') {
          prefix = null
          localName = text.slice(p, q)
        }
        else {
          prefix = text.slice(p, q)
          q += 1
          p = q
          while (text(q) > 63 || text.attributeName(text(q)) > 0)
            q += 1
          localName = text.slice(p, q)
        }

        point = q
        typ = XmlTokenType.StartElement
        return typ
    }
  }

  def doAttributeName(): Int = {
    var p = point
    var q = p
    var e = text.size

    while (text(q) > 63 || text.attributeName(text(q)) > 0)
      q += 1
    if (q >= e)
      return endOfInput()

    if (text(q) == ':') {
      prefix = text.slice(p, q)
      q += 1
      p = q

      while (text(q) > 63 || text.attributeName(text(q)) > 0 )
        q += 1

      localName = text.slice(p, q)
    }
    else {
      prefix = null
      localName = text.slice(p, q)
    }

    if (text(q) <= 32) {
      q += 1
      while (text(q) <= 32) {
        q += 1
      }
      if (q >= e)
        return endOfInput()
    }

    if (text(q) == '=') {
      q += 1
      while (text(q) <= 32) {
        q += 1
      }
      if (q >= e)
        return endOfInput()

      val quote = text(q)
      quote match {
        case '"' | '\'' =>
          p = q + 1
          q += 1
          while (text(q) != quote) {
            q += 1
          }
          if (q < e) {
            rawValue = text.slice(p, q - p)
            point = q + 1; // skip end quote
            typ = XmlTokenType.Attribute
            return typ
          }
          return endOfInput()

        case _ =>
          return doExpected("\' or \"", q)
      }
    }

    doExpected("=", q)
  }

  def doEndEmptyElement(): Int = {
    if (text(point) == '/' && text(point + 1) == '>') {
      localName = null
      prefix = null
      point += 2
      typ = XmlTokenType.EndEmptyElement
      return typ
    }
    doExpected("/>", text.point)
  }

  /** *********************************************************************

    * **********************************************************************/

  def doComment(): Int = {
    var e = text.size
    var p = point
    var q = p

    while (p < e) {
      while (text(p) != '-') {
        p += 1
        if (p >= e)
          return endOfInput()
      }

      if (text.slice(p, p + 3) sameElements "-->".toCharArray) {
        point = p + 3
        rawValue = text.slice(q, p)
        typ = XmlTokenType.Comment
        return typ
      }
      p += 1
    }

    endOfInput()
  }


  def doCData(): Int = {
    val e = text.size
    var p = point

    while (p < e) {
      var q = p
      while (text(p) != ']') {
        p += 1
        if (text(p) >= e)
          return endOfInput()
      }

      if (text.slice(p, p + 3) sameElements "]]>".toCharArray) {
        point = p + 3
        rawValue = text.slice(q, p)
        typ =XmlTokenType.CData
        return typ
      }
      p += 1
    }

    endOfInput()
  }

  def doPI(): Int = {
    var e = end
    var p = point
    var q = p

    while (p < e) {
      while (text(p) != '?') {
        p += 1
        if (p >= e)
          return endOfInput()
      }

      if (text(p + 1) == '>') {
        rawValue = text.slice(q, p)
        point = p + 2
        typ = XmlTokenType.PI
        return typ
      }
      p += 1
    }
    endOfInput()
  }

  def doDoctype(): Int = {
    var e = text.size
    var p = point

    // strip leading whitespace
    while (text(p) <= 32) {
      p += 1
      if (text(p) >= e)
        return endOfInput()
    }

    var q = p
    while (p < e) {
      if (text(p) == '>') {
        rawValue = text.slice(q, p)
        prefix = null
        point = p + 1
        typ = XmlTokenType.Doctype
        return typ
      }
      else {
        if (text(p) == '[')
          do {
            p += 1
            if (p >= e)
              return endOfInput()
          } while (text(p) != ']')
        p += 1
      }
    }

    if (p >= e)
      endOfInput()
    else
    typ = XmlTokenType.Doctype
    typ
  }

  def doUnexpected(msg: String, p: Int): Int = {
    position("parse error :: unexpected  " + msg, p)
    -1
  }

  /** *********************************************************************

    * **********************************************************************/

  def doExpected(msg: String, p: Int): Int = {
    position("parse error :: expected  " + msg + " instead of " + text.slice(p, p + 1), p)
    -1
  }

  /** *********************************************************************

    * **********************************************************************/

  def position(msg: String, p: Int): Int = {
    error(msg + " at position " + p + ", source: "
      + new String(text.slice(Math.max(0, p-10), p))
      +"^"+
      new String(text.slice(p, Math.min(text.size, p+10))))
    -1
  }

  /** *********************************************************************

    * **********************************************************************/

  def error (msg: String): Int = {
    errMsg = msg
    throw new XmlException(msg)
    -1
  }

  /** *********************************************************************

                Return the raw value of the current token

    * **********************************************************************/

  def value = {
    rawValue
  }

  /** *********************************************************************

                Return the name of the current token

    * **********************************************************************/

  def name = {
    if (prefix.nonEmpty)
      prefix + ":" + localName
    else localName
  }

  /** *********************************************************************

                Returns the text of the last error

    * **********************************************************************/

  def error() = {
    errMsg
  }

  /** *********************************************************************

                Reset the parser

    * **********************************************************************/

  def reset() = {
    //text.reset (text.text)
    reset_()
    true
  }

  /** *********************************************************************

                Reset parser with new content

    * **********************************************************************/

  def reset(newText: Array[Char])= {
    text.reset(newText)
    reset_()
  }

  /** *********************************************************************

                experimental: set streaming mode

                Use at your own risk, may be removed.

    * **********************************************************************/

  def incremental(yes: Boolean = true) {
    stream = yes
  }

  /** *********************************************************************

    * **********************************************************************/

  def reset_() = {
    depth = 0
    errMsg = null
    typ = XmlTokenType.None

    var p = point

    // consume UTF8 BOM
    if (text(p) == 0xef && text(p + 1) == 0xbb && text(p + 2) == 0xbf)
      p += 3


    //TODO enable optional declaration parsing
    var e = end
    while (p < e && text(p) <= 32)
      p += 1

    if (p < e)
      if (text(p) == '<' && text(p + 1) == '?' && text.slice(p + 2, p + 5) == "xml".toCharArray) {
        p += 5
        while (p < e && text(p) != '?')
          p += 1
        p += 2
      }
    point = p
  }

  def endOfInput () =  {
    if (depth > 0 && (!stream) )
      error ("Unexpected EOF")

   typ = XmlTokenType.Done
   typ
  }

}


/** *****************************************************************************

  * ******************************************************************************/
class XmlText() {
  var end: Int = _
  var len: Int = _
  var text: Array[Char] = _
  var point: Int = _

  def apply(pos: Int) = {
    text(pos)
  }

  def slice(from: Int, to: Int) =
    text.slice(from, to)

  def size = text.size

  def reset(newText: Array[Char]) {
    this.text = newText
    this.len = newText.length
    this.point = 0
    this.end = point + len
  }

  val name = Array[Byte](
    // 0   1   2   3   4   5   6   7   8   9   A   B   C   D   E   F
    0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 0, 1, 1, // 0
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, // 1
    0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, // 2
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 0 // 3
    )

  val  attributeName = Array[Byte](
    // 0   1   2   3   4   5   6   7   8   9   A   B   C   D   E   F
    0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 0, 1, 1, // 0
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, // 1
    0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, // 2
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0 // 3
  )

}


object Whitespace {
  val StripWhite = 0
  val PartialWhite = 1
}

object XmlNodeType {
  val Element = 0
  val Data = 1
  val Attribute = 2
  val CData = 3
  val Comment = 4
  val PI = 5
  val Doctype = 6
  val Document = 7
}

object XmlTokenType {
  val Done = 0
  val StartElement = 1
  val Attribute = 2
  val EndElement = 3
  val EndEmptyElement = 4
  val Data = 5
  val Comment = 6
  val CData = 7
  val Doctype = 8
  val PI = 9
  val None = 10
}

class XmlException(s: String) extends IOException(s)
