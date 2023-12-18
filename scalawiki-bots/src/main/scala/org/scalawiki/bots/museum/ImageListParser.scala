package org.scalawiki.bots.museum

import fr.opensagres.poi.xwpf.converter.core.FileURIResolver
import fr.opensagres.poi.xwpf.converter.xhtml.{XHTMLConverter, XHTMLOptions}

import java.io.{ByteArrayOutputStream, File, FileInputStream}
import org.apache.poi.ooxml.POIXMLDocument
import org.apache.poi.hwpf.converter.WordToHtmlConverter
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import better.files.{File => SFile}

object ImageListParser {

  def getDocText(listFile: File): String = {
    val extractor = if (listFile.getName.toLowerCase.endsWith(".docx"))
      new XWPFWordExtractor(POIXMLDocument.openPackage(listFile.getAbsolutePath))
    else
      new WordExtractor(new FileInputStream(listFile))

    extractor.getText
  }

  def docToHtml(docs: Seq[File]): Unit = {
    docs.foreach {
      doc =>
        val low = doc.getName.toLowerCase
        val fullname = doc.getAbsolutePath
        if (low.endsWith(".doc")) {
          val html = fullname.replace(".doc", ".html")
          if (!new File(html).exists()) {
            WordToHtmlConverter.main(Array(fullname, html))
          }
        } else if (low.endsWith(".docx")) {
          val html = fullname.replace(".docx", ".html")
          if (!new File(html).exists()) {
            docxToHtml(fullname, html)
          }
        }
    }
  }

  def docxToHtml(docFile: String, htmlFile: String) = {
    val in = new FileInputStream(new File(docFile))
    val document = new XWPFDocument(in)
    val options = XHTMLOptions.create().URIResolver(new FileURIResolver(new File("word/media")))

    val out = new ByteArrayOutputStream()


    XHTMLConverter.getInstance().convert(document, out, options)
    val text = out.toString()

    SFile(htmlFile).overwrite(text)
  }

}
