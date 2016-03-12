package org.scalawiki

object LoginInfo {
  def login = System.getenv.get("SCALAWIKI_LOGIN")
  def password = System.getenv.get("SCALAWIKI_PASSWORD")
}
