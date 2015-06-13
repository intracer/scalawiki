package org.scalawiki.sql

import slick.driver.H2Driver.api._

/**
 * https://www.mediawiki.org/wiki/Manual:Text_table
 * The text table holds the wikitext of individual page revisions.
 * If using Postgres or Oracle, this table is named pagecontent.
 * Field names are a holdover from the 'old' revisions table in MediaWiki 1.4 and earlier.
 * @param tag
 */
class Texts(tag: Tag, tableName: String) extends Table[Text](tag, tableName) {
  /**
   * revision.rev_text_id in revision table is a key to this column.
   * (In MediaWiki 1.5+, archive.ar_text_id is also a key to this column.)
   * @return
   */
  def id = column[Option[Long]]("old_id", O.PrimaryKey, O.AutoInc)

  /**
   * The wikitext of the page
   * @return
   */
  def text = column[String]("old_text", O.NotNull)

  /**
   * Comma-separated list of flags. Contains the following possible values:
   * <dl>
   * <dt> gzip </dt>
   * <dd>Text is compressed with PHP's gzdeflate() function.
   * Note: If the $wgCompressRevisions option is on, new rows (=current revisions) will be gzipped transparently at save time.
   * Previous revisions can also be compressed by using the script compressOld.php
   * </dd>
   * <dt> utf-8 </dt>
   * <dd> Text was stored as UTF-8.
   * Note: If the $wgLegacyEncoding option is on, rows *without* this flag will be converted to UTF-8 transparently at load time.
   * </dd>
   * <dt> object </dt>
   * <dd>Text field contained a serialized PHP object.
   * Note: The object either contains multiple versions compressed together to achieve a better compression ratio,
   * or it refers to another row where the text can be found.
   * </dd>
   * <dt> external </dt>
   * <dd>Text was stored in an external location specified by old_text.
   * Note: Any additional flags apply to the data stored at that URL, not the URL itself.
   * The 'object' flag is not set for URLs of the form 'DB://cluster/id/itemid',
   * because the external storage system itself decompresses these.
   * </dd>
   * </dl>
   * @return
   */
  def flags = column[String]("old_flags")

  def * = (id, text, flags) <>(Text.tupled, Text.unapply)

}

case class Text(id: Option[Long], text: String, flags: String = "utf-8")


