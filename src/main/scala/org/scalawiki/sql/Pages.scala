package org.scalawiki.sql

import org.scalawiki.dto.Page

import scala.slick.driver.H2Driver.simple._

/**
 * https://www.mediawiki.org/wiki/Manual:Page_table
 * Each page in a MediaWiki installation has an entry here which identifies it by title and contains some essential metadata.
 * It was first introduced in r6710, in MediaWiki 1.5. The text of the page itself is stored in the text table.
 * To retrieve the text of an article, MediaWiki first searches for page_title in the page table.
 * Then, page_latest is used to search the revision table for rev_id, and rev_text_id is obtained in the process.
 * The value obtained for rev_text_id is used to search for old_id in the text table to retrieve the text.
 * When a page is deleted, the revisions are moved to the archive table
 * @param tag
 */
class Pages(tag: Tag, tableName: String, val dbPrefix: Option[String]) extends Table[Page](tag, tableName) {

  def withPrefix(name: String) = dbPrefix.fold("")(_ + "_") + name
  /**
   * Uniquely identifying primary key. This value is preserved across edits and renames.
   * There is an analogous field in the archive table to preserve this value in MediaWiki 1.11 and later; however,
   * it is [[https://phabricator.wikimedia.org/T28123 not used]] in Special:Undelete, the interface for undeleting pages used by project administrators.
   * @return
   */
  def id = column[Option[Long]]("page_id", O.PrimaryKey, O.AutoInc)

  /**
   * A page name is broken into a [[https://www.mediawiki.org/wiki/Manual:Namespace namespace]] and a title.
   * The namespace keys are UI-language-independent constants, defined in includes/Defines.php.
   * This field contains the number of the page's namespace. The values range from 0 to 15 for the standard namespaces,
   * and from 100 to 2147483647 for [[https://www.mediawiki.org/wiki/Manual:Using_custom_namespaces custom namespaces]].
   * @return
   */
  def namespace = column[Int]("page_namespace", O.NotNull)

  /**
   * The sanitized [[https://www.mediawiki.org/wiki/Manual:Page_title page title]], without the title of its namespace
   * with a maximum of 255 characters (binary), e.g. "[255 chars]" or "Talk:[255 chars]" or "Category discussion:[255 chars here]".
   * It is stored as text, with spaces replaced by underscores. The real title shown in articles is just this title
   * with underscores (_) converted to spaces ( ).
   * @return
   */
  def title = column[String]("page_title", O.NotNull)

  def restrictions = column[String]("page_restrictions", O.NotNull)

  def counter = column[Int]("page_counter", O.NotNull)

  def isRedirect = column[Boolean]("page_is_redirect", O.NotNull)

  def isNew = column[Boolean]("page_is_new", O.NotNull)

  def random = column[Double]("page_random", O.NotNull)

  def touched = column[String]("page_touched", O.NotNull)

  def linksUpdated = column[String]("page_links_updated")

  def pageLatest = column[Long]("page_latest", O.NotNull)

  def pageLen = column[Int]("page_len", O.NotNull)

  def contentModel = column[String]("page_content_model")

  def lang = column[String]("page_lang")

  def nameTitle = index(withPrefix("name_title"), (namespace, title), unique = true)

//  def revision = foreignKey("revisionFK", pageLatest, MediaWiki.revisions)(_.id)

  def * = (id, namespace, title, pageLatest) <>(fromDb, toDb)

  def fromDb(t: (Option[Long], Int, String, Long)) =
    Page.noText(
      id = t._1.get,
      ns = t._2,
      title = t._3
    )

  def toDb(p: Page) = Some((
    p.id,
    p.ns,
    p.title,
    p.revisions.head.id.getOrElse(0L)
    ))
}