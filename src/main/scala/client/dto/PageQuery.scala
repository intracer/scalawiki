package client.dto

import client.MwUtils

class PageQuery(query: Either[Set[Long], Set[String]]) {
  def toMap(paramNames:(String, String)): Map[String, String] = {
    val paramValues:Set[String] = query.fold(_.map(_.toString), identity)
    val paramName = query.fold(_ => paramNames._1, _ => paramNames._2)
    Map(paramName -> paramValues.map(MwUtils.normalize).mkString("|"))
  }
}

//class PagesQuery(query: Either[Set[Int], Set[String]]) extends PageQuery(query)
class SinglePageQuery(query: Either[Long, String]) extends PageQuery(query.fold(id => Left(Set(id)), title => Right(Set(title))))


object PageQuery {
  def byTitles(titles: Set[String]) = new PageQuery(Right(titles))

  def byTitle(title: String) = new SinglePageQuery(Right(title))

  def byIds(ids: Set[Long]) = new PageQuery(Left(ids))

  def byId(id: Long) = new SinglePageQuery(Left(id))
}

