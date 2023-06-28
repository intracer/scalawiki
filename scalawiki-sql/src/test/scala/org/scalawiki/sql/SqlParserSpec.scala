package org.scalawiki.sql

import net.sf.jsqlparser.expression.{LongValue, RowConstructor, StringValue}
import net.sf.jsqlparser.expression.operators.relational.ExpressionList
import org.scalawiki.util.TestUtils.resourceAsString
import org.specs2.mutable.Specification
import net.sf.jsqlparser.parser.{CCJSqlParser, CCJSqlParserUtil, StringProvider}
import net.sf.jsqlparser.statement.insert.Insert
import net.sf.jsqlparser.statement.select.SetOperationList
import net.sf.jsqlparser.statement.values.ValuesStatement

import scala.collection.mutable
import scala.jdk.CollectionConverters.ListHasAsScala

case class SqlCategory(id: Long,
                       title: String,
                       pages: Long,
                       subCats: Long,
                       files: Long)
class SqlParserSpec extends Specification {

  "parser" should {
    "parse categories" in {
//      val sqlStr = resourceAsString("/category.sql")
//        .split("\n")
//        .filter(_.startsWith("INSERT INTO"))
//        .head

      val sqlStr = "INSERT INTO `category` VALUES " +
        "(1,'NowCommons',0,0,0)," +
        "(2,'Зображення:Герби_міст_України',7,0,7)," +
        "(3,'Суспільне_надбання',13896,18,13872);"

      val parser = new CCJSqlParser(new StringProvider(sqlStr))
        .withBackslashEscapeCharacter(true)
        .withTimeOut(30000)
      val statement =
        CCJSqlParserUtil.parseStatement(parser).asInstanceOf[Insert]

      val categories = statement.getSelect.getSelectBody
        .asInstanceOf[SetOperationList]
        .getSelects
        .get(0)
        .asInstanceOf[ValuesStatement]
        .getExpressions
        .asInstanceOf[ExpressionList]
        .getExpressions
        .asScala
        .map {
          case row: RowConstructor =>
            val expressions = row.getExprList.getExpressions.asScala
            expressions match {
              case mutable.Buffer(id: LongValue,
                                  title: StringValue,
                                  pages: LongValue,
                                  subCats: LongValue,
                                  files: LongValue) =>
                SqlCategory(id.getValue,
                            title.getValue,
                            pages.getValue,
                            subCats.getValue,
                            files.getValue)
            }
        }
      categories === mutable.Buffer(
        SqlCategory(1, "NowCommons", 0, 0, 0),
        SqlCategory(2, "Зображення:Герби_міст_України", 7, 0, 7),
        SqlCategory(3, "Суспільне_надбання", 13896, 18, 13872)
      )
    }
  }

}
