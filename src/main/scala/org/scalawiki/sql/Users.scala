package org.scalawiki.sql

import org.scalawiki.dto.User

import slick.driver.H2Driver.api._

/**
 * https://www.mediawiki.org/wiki/Manual:User_table
 * The user table is where MediaWiki stores information about users. If using Postgres, this table is named mwuser.
 * @param tag
 */
class Users(tag: Tag, tableName: String, val dbPrefix: Option[String]) extends Table[User](tag, tableName) {

  def withPrefix(name: String) = dbPrefix.fold("")(_ + "_") + name

  def id = column[Option[Long]]("user_id", O.PrimaryKey, O.AutoInc)

  /**
   * Usernames must be unique, and must not be in the form of an IP address.
   * Shouldn't allow slashes or case conflicts. Spaces are allowed, and are not converted to underscores like titles.
   * @return
   */
  def name = column[String]("user_name", O.NotNull)

  /**
   * stores the user's real name (optional) as provided by the user in their "Preferences" section.
   * @return
   */
  def realName = column[String]("user_real_name")

  /**
   * user_password is one of three formats, depending on the setting of $wgPasswordSalt and $wgPasswordDefault:
   * -Since MediaWiki 1.24, $wgPasswordDefault defaults to pbkdf2. In this case you will get a concatenation of:
   * The string ":pbkdf2:".
   * The hashing algorithm used inside the pbkdf2 layer, by default "sha256".
   * The colon character (":").
   * The cost for this algorithm, by default "10000".
   * The colon character (":").
   * The length (OF WHAT?), by default "128".
   * The colon character (":").
   * Another string, e.g. "kkdejKlBYFV7+LP2m2thYA=="
   * And finally another key.
   * -Since MediaWiki 1.24, if the maintenance script wrapOldPasswords.php has been used, passwords may also start with ":pbkdf2-legacyA:" or ":pbkdf2-legacyB:" like ":pbkdf2-legacyB:!sha256:10000:128!...".
   * -In MediaWiki 1.23 and older, if $wgPasswordSalt is true (default) it is a concatenation of:
   * The string ":B:",
   * A pseudo-random hexadecimal 31-bit salt between 0x0 and 0x7fff ffff (inclusive),
   * The colon character (":"), and
   * The MD5 hash of a concatenation of the salt, a dash ("-"), and the MD5 hash of the password.
   * -In MediaWiki 1.23 and older, if $wgPasswordSalt is false, it is a concatenation of:
   * The string ":A:" and
   * The MD5 hash of the password.
   * @return
   */
  def password = column[Int]("user_password")

  /**
   * newPassword is generated for the mail-a-new-password feature
   * @return
   */

  def newPassword = column[String]("user_newpassword")

  /**
   * newPassTime is set to the current timestamp (wfTimestampNow()) when a new password is set. Like the other timestamps,
   * it is in in MediaWiki's timestamp format (yyyymmddhhmmss, e.g. 20130824025644).
   * @return
   */

  def newPassTime = column[Int]("user_newpass_time")

  /**
   *
   * @return
   */

  def email = column[String]("user_email")

  /**
   * touched is the last time a user made a change on the site, including logins, changes to pages (any namespace), watchlistings, and preference changes.
   * note: the user_touched time resets when a user is left a talkpage message.
   * @return
   */
  def touched = column[String]("user_touched")

  /**
   * token is a pseudorandomly generated value. When a user checks "Remember my login on this browser" the value is
   * stored in a persistent browser cookie ${wgCookiePrefix}Token that authenticates the user while being resistant to spoofing.
   * @return
   */
  def token = column[Boolean]("user_token")

  /**
   *
   * @return
   */
  def emailAuthenticated = column[Boolean]("user_email_authenticated")

  /**
   *
   * @return
   */
  def emailToken = column[Int]("user_email_token")

  /**
   *
   * @return
   */
  def emailTokenExpires = column[Int]("user_email_token_expires")

  /**
   *
   * @return
   */
  def userRegistration = column[String]("user_registration")

  /**
   *
   * @return
   */
  def userEditcount = column[String]("user_editcount")

  /**
   *
   * @return
   */
  def userPasswordExpires = column[String]("user_password_expires")

  def nameIndex = index(withPrefix("user_name"), name, unique = true)

  def * = (id, name) <>(fromDb, toDb)

  def fromDb(t: (Option[Long], String)) =
    User(
      id = t._1,
      login = Some(t._2)
    )

  def toDb(u: User) = Some((
    u.id,
    u.login.orNull
    ))
}

