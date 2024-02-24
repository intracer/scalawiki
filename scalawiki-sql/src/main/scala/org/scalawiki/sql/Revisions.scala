package org.scalawiki.sql

import org.scalawiki.dto.{Contributor, Revision}

import slick.driver.H2Driver.api._

/** https://www.mediawiki.org/wiki/Manual:Revision_table The revision table
  * holds metadata for every edit done to a page within the wiki. Every edit of
  * a page creates a revision row, which holds information such as the user who
  * made the edit, the time at which the edit was made, and a reference to the
  * new wikitext in the text table.
  * @param tag
  */
class Revisions(tag: Tag, tableName: String, val dbPrefix: Option[String])
    extends Table[Revision](tag, tableName) {

  def withPrefix(name: String) = dbPrefix.fold("")(_ + "_") + name

  /** This field holds the primary key for each revision. page_latest is a
    * foreign key to this field.
    * @return
    */
  def id = column[Option[Long]]("rev_id", O.PrimaryKey, O.AutoInc)

  /** This field holds a reference to the page to which this revision pertains.
    * The number in this field is equal to the page_id field of said page.
    * @return
    */
  def pageId = column[Long]("rev_page")

  /** This is a foreign key to old_id in the text table. (The text table is
    * where the actual bulk text is stored.) It's possible for multiple
    * revisions to use the same text—for instance, revisions where only metadata
    * is altered, or where a rollback is done to a previous version.
    * @return
    */
  def textId = column[Long]("rev_text_id")

  /** This field holds an editor's edit summary (editor's comment on revision).
    * This text is shown in the history and contributions. (The recentchanges
    * table contains a copy used for recent changes, related changes,
    * watchlists, and, in the case of page creation, for the list of new pages.)
    * It is rendered in a sanitized subset of wiki markup.
    * @return
    */
  def comment = column[String]("rev_comment")

  /** This is equal to the user_id of the user who made this edit. The value for
    * this field is 0 for anonymous edits, initializations scripts, and for some
    * mass imports.
    * @return
    */
  def userId = column[Long]("rev_user", O.Default(0))

  /** This field holds the text of the editor's username, or the IP address of
    * the editor if the revision was done by an unregistered user. In anonymous
    * revisions imported from UseModWiki or early incarnations of the Phase II
    * software, this field may contain an IP address with the final octet
    * obscured (i.e. \d{1,3}\.\d{1,3}\.\d{1,3}\.xxx such as 24.150.61.xxx; see
    * bug 3631). Some edits imported from UseModWiki may contain a Reverse DNS
    * lookup hostname like ppfree165-153-bz.aknet.it or office.bomis.com.
    * @return
    */
  def userText = column[String]("rev_user_text", O.Default(""))

  /** Holds the timestamp of the edit.
    * @return
    */
  def timestamp = column[String]("rev_timestamp", O.Default(""))

  /** Records whether the user marked the 'minor edit' checkbox. If the value
    * for this field is 1, then the edit was declared as 'minor'; it is 0
    * otherwise. Many automated edits are marked as minor.
    * @return
    */
  def minorEdit = column[Boolean]("rev_minor_edit", O.Default(false))

  /** This field is reserved for the
    * [[https://www.mediawiki.org/wiki/Manual:RevisionDelete RevisionDelete system]].
    * It's a bitfield in which the values are DELETED_TEXT = 1; DELETED_COMMENT
    * \= 2; DELETED_USER = 4; and DELETED_RESTRICTED = 8. So, for example, if
    * nothing has been deleted from that revision, then the value is 0; if both
    * the comment and user have been deleted, then the value is 6.
    * @return
    */
  def deleted = column[Int]("rev_deleted", O.Default(0))

  /** This field contains the length of the article after the revision, in
    * bytes.
    * @return
    */
  def len = column[Long]("rev_len")

  /** The rev_id of the previous revision to the page. Corresponds to
    * rc_last_oldid. For edits which are new page creations, rev_parent_id = 0.
    * @return
    */
  def parentId = column[Long]("rev_parent_id")

  /** This field is used to add the SHA-1 text content hash in base-36
    * @return
    */
  def sha1 = column[String]("rev_sha1")

  def contentModel = column[String]("rev_content_model")

  def contentFormat = column[String]("rev_content_format")

  def pageRevId = index(withPrefix("rev_page_id"), (pageId, id), unique = true)

  //  def timestampIndex = index("rev_timestamp", timestamp)
  //  def pageTampstamp = index("page_timestamp", (pageId, timestamp))
  //  def userTimestamp = index("user_timestamp", (userId, timestamp))
  //  def userTextTimestamp = index("usertext_timestamp", (userText, timestamp))

  // def pageUserTimestamp = index("page_user_timestamp", (pageId, userId, timestamp))

  //  def page = foreignKey("pageFK", pageId, MediaWiki.pages)(_.id)
  //
  //  def text = foreignKey("textFK", textId, MediaWiki.pages)(_.id)
  //
  //  def user = foreignKey("userFK", userId, MediaWiki.users)(_.id)

  def * = (id, pageId, parentId, textId, userId, userText) <> (fromDb, toDb)

  def fromDb(t: (Option[Long], Long, Long, Long, Long, String)) =
    Revision(
      revId = t._1,
      pageId = Option(t._2),
      parentId = Option(t._3),
      textId = Option(t._4),
      user = Contributor(Option(t._5), Option(t._6))
    )

  def toDb(r: Revision) = Some(
    (
      r.id,
      r.pageId.get,
      r.parentId.getOrElse(0L),
      r.textId.get,
      r.user.flatMap(_.id).getOrElse(0L),
      r.user.flatMap(_.name).getOrElse("")
    )
  )
}
