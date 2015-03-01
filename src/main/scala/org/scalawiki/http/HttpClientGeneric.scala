package org.scalawiki.http

trait HttpClientGeneric {

  def get(params: Map[String, String]): String

}


abstract class HttpClientGenericImpl extends HttpClient {

}
