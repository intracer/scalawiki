package client.dto

case class LoginResponse(result: String,
                          token: Option[String],
                          lgToken: Option[String],
                          lguserid: Option[Int],
                          lgusername: Option[String],
                          cookieprefix: Option[String],
                          sessionid: Option[String])

//  {"login":{"result":"NeedToken","token":"a504e9507bb8e8d7d3bf839ef096f8f7","cookieprefix":"ukwiki","sessionid":"37b1d67422436e253f5554de23ae0064"}}
