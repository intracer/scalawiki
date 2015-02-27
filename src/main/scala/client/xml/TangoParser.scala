package client.xml

import akka.util.ByteString


class TangoParser(val stripWhite: Boolean) {
  type XmlTokenType = Int

  var depth: Int = _
  var prefix: ByteString = _
  var rawValue: ByteString = _
  var localName: ByteString = _
  var typ: Int = XmlTokenType.None

  var text: ByteString = _
  var point: Int = 0
  var stream: Boolean = _
  var errMsg: ByteString = _
  var content: ByteString = _

  val retainWhite = !stripWhite
  val partialWhite = !stripWhite

  def next(): XmlTokenType =
  {
    var e = text.size
    var p = point

    // at end of document?
    if (p >= e)
      return endOfInput()
    if (stripWhite)
    {
      // strip leading whitespace
      while (text(p) <= 32) {
        p += 1
        if (p >= e)
          return endOfInput()
      }
    }
    // StartElement or Attribute?
    if (typ < XmlTokenType.EndElement)
    {
      if (retainWhite)
      {
        // strip leading whitespace (thanks to DRK)
        while (text(p) <= 32) {
          p += 1
          if (p >= e)
            return endOfInput()
        }
      }
      text(p) match
      {
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
    if (text(p) != '<')
    {
      var q = p
      p+=1
      while (p < e && text(p) != '<') { p+=1 }

      if (p < e)
      {
        if (partialWhite)
        {
          // include leading whitespace
          while (text(q-1) <= 32)
            q-=1
        }
        point = p
        rawValue = text.slice(q, p - q)
        return XmlTokenType.Data
      }
      return endOfInput()
    }

    // must be a '<' character, so peek ahead
     text(p+1) match
    {
      case '!' =>
      // one of the following ...
      if (text.slice(p+2, p+4) == "--".getBytes("UTF-8"))
      {
        point = p + 4
        return doComment()
      }
      else
      if (text.slice(p+2, p+9) == "[CDATA[".getBytes("UTF-8"))
      {
        point = p + 9
        return doCData()
      }
      else
      if (text.slice(p+2, p+9) == "DOCTYPE".getBytes("UTF-8"))
      {
        point = p + 9
        return doDoctype()
      }
      return doUnexpected("!", p)

      case '?' =>
      // must be PI data
      point = p + 2
      return doPI()

      case '/' =>
      // should be a closing element name
      p += 2
      var q = p
      while (text(q) > 63 /* || text.name[*q]*/)
       q+=1
      if (text(q) == ':')
      {
        prefix = text.slice(p,  q)
        q += 1
        p = q
        while (text(q) > 63 /*|| text.attributeName[*q]*/)
         q+=1

        localName = text.slice(p, q)
      }
      else
      {
        prefix = null
        localName = text.slice(p, q)
      }

      while (text(q) <= 32) {
        q+=1
        if (q >= e)
          return endOfInput()
      }

      if (text(q) == '>')
      {
        depth -=1
        point = q + 1
        return XmlTokenType.EndElement
      }
      return doExpected(">", q)

      case _ =>
        // scan new element name
        p+=1
        var q = p
      while (text(q) > 63 /*|| text.name[*q]*/)
      q+=1

      // check if we ran past the end
      if (q >= e)
        return endOfInput()

      if (text(q) != ':')
      {
        prefix = null
        localName = text.slice(p, q)
      }
      else
      {
        prefix = text.slice(p, q)
        q += 1
        p = q
        while (text(q) > 63 /*|| text.attributeName[*q]*/)
          q+=1
        localName = text.slice(p, q)
      }

      point = q
      return XmlTokenType.StartElement
    }
  }

  def  doAttributeName()
  {
    var p = point
    var q = p
    var e = text.size

    while (text(q) > 63 /*|| text.attributeName[*q]*/)
      q+=1
    if (q >= e)
      return endOfInput()

    if (text(q) == ':')
    {
      prefix = text.slice(p, q)
      q +=1
      p = q

      while (text(q) > 63 /*|| text.attributeName[*q]*/)
      q+=1;

      localName = text.slice(p, q)
    }
    else
    {
      prefix = null;
      localName = text.slice(p, q)
    }

    if (text(q) <= 32)
    {
      q += 1
      while (text(q) <= 32) { q+=1}
      if (q >= e)
        return endOfInput()
    }

    if (text(q) == '=')
    {
      q+=1
      while (text(q) <= 32) {q+=1}
      if (q >= e)
        return endOfInput()

      val quote = text(q)
      quote match
      {
        case '"' | '\'' =>
            p = q + 1
          q+=1
        while (text(q) != quote) {q+=1}
        if (q < e)
        {
          rawValue = text.slice(p, q - p)
          point = q + 1;   // skip end quote
          return XmlTokenType.Attribute
        }
        return endOfInput()

        case _ =>
        return doExpected("\' or \"", q)
      }
    }

    return doExpected ("=", q)
  }

  def doEndEmptyElement()
  {
    if (text(point) == '/' && text(point + 1) == '>')
    {
      localName = null
      prefix = null
      point += 2
      return XmlTokenType.EndEmptyElement
    }
    doExpected("/>", text.point)
  }

  /***********************************************************************

    ***********************************************************************/

  def doComment()
  {
    var e = text.size
    var p = point
    var q = p

    while (p < e)
    {
      while (text(p) != '-') {
        p+=1
        if (p >= e)
          return endOfInput()
      }

      if (text.slice(p, p+3) == ByteString("-->".getBytes("UTF-8")))
      {
        point = p + 3
        rawValue = text.slice(q, p)
        return XmlTokenType.Comment
      }
      p+=1
    }

    return endOfInput()
  }


}

object Whitespace {
  val StripWhite = 1
  val PartialWhite = 2
}

object XmlNodeType {
  val Element = 1
  val Data = 2
  val Attribute = 3
  val CData = 4
  val Comment = 5
  val PI = 6
  val Doctype = 7
  val Document = 8
}

object XmlTokenType {
  val Done = 1
  val StartElement = 2
  val Attribute = 3
  val EndElement = 4
  val EndEmptyElement = 5
  val Data = 6
  val Comment = 7
  val CData = 8
  val Doctype = 9
  val PI = 10
  val None = 11
}