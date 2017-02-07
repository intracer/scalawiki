package org.scalawiki


trait LoginInfo {
  def login: String

  def password: String
}

case class LoginInfoValue(login: String, password: String) extends LoginInfo

object LoginInfo {

  def fromEnv(
               loginProp: String = "SCALAWIKI_LOGIN",
               passwordProp: String = "SCALAWIKI_PASSWORD"): Option[LoginInfo] =
    for (
      login <- sys.env.get(loginProp);
      password <- sys.env.get(passwordProp)
    ) yield
      LoginInfoValue(login, password)

}
