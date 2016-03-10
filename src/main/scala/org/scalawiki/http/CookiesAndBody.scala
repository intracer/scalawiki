package org.scalawiki.http

import spray.http.HttpCookie

case class CookiesAndBody(cookies: List[HttpCookie], body: String)
