package org.scalawiki.sql

import slick.driver.H2Driver.api._

/**
 * https://www.mediawiki.org/wiki/Manual:Category_table
 * Track all existing categories. Something is a category if
 * it has an entry somewhere in categorylinks, or
 * it once did (Task T28411).
 * Categories might not have corresponding pages, so they need to be tracked separately. cat_pages, cat_subcats, and cat_files are signed to make underflow more obvious.
 * Note The pages and sub-categories are stored in the categorylinks table.
 * Note: Information regarding which categories are hidden is stored in the page_props table.
 * @param tag
 */

class Categories(tag: Tag, tableName: String) extends Table[Category](tag, tableName) {

  def id = column[Long]("cat_id", O.PrimaryKey)

  /**
   * Name of the category, in the same form as page.page_title (with underscores).
   * If there is a category page corresponding to this category, by definition,
   * it has this name (in the Category namespace).
   * @return
   */
  def title = column[String]("cat_title")

  /**
   * Number of pages in the category. This number includes the number of subcategories and the number of files.
   * @return
   */
  def pages = column[Int]("cat_pages")

  /**
   * Number of sub-categories in the category.
   * @return
   */
  def subCats = column[Int]("cat_subcats")

  /**
   * Number of files (i.e. Image: namespace members) in the category.
   * @return
   */
  def files = column[Int]("cat_files")

  def * = (id, title, pages, subCats, files) <>(Category.tupled, Category.unapply)
}

case class Category(id: Long, title: String, pages: Int, subCats: Int, files: Int)
