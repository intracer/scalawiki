package org.scalawiki.duckdb

import io.getquill._
import org.scalawiki.dto.Image
import java.time.ZonedDateTime
import javax.sql.DataSource

/**
 * Repository for reading and writing Image instances using Quill to DuckDB database
 */
class ImageRepository(dataSource: DataSource) {

  // DuckDB is compatible with PostgreSQL dialect for most operations
  private val ctx = new PostgresJdbcContext(NamingStrategy(SnakeCase), dataSource)
  import ctx._

  /**
   * Database schema for Image
   */
  case class ImageRow(
      title: String,
      url: Option[String] = None,
      pageUrl: Option[String] = None,
      size: Option[Long] = None,
      width: Option[Int] = None,
      height: Option[Int] = None,
      author: Option[String] = None,
      uploaderLogin: Option[String] = None,
      year: Option[String] = None,
      date: Option[ZonedDateTime] = None,
      monumentIds: String = "",
      pageId: Option[Long] = None,
      categories: String = "",
      specialNominations: String = "",
      mime: Option[String] = None
  )

  /**
   * Convert Image DTO to database row
   */
  private def toImageRow(image: Image): ImageRow = {
    ImageRow(
      title = image.title,
      url = image.url,
      pageUrl = image.pageUrl,
      size = image.size,
      width = image.width,
      height = image.height,
      author = image.author,
      uploaderLogin = image.uploader.flatMap(_.login),
      year = image.year,
      date = image.date,
      monumentIds = image.monumentIds.mkString(","),
      pageId = image.pageId,
      categories = image.categories.mkString(","),
      specialNominations = image.specialNominations.mkString(","),
      mime = image.mime
    )
  }

  /**
   * Convert database row to Image DTO
   */
  private def fromImageRow(row: ImageRow): Image = {
    Image(
      title = row.title,
      url = row.url,
      pageUrl = row.pageUrl,
      size = row.size,
      width = row.width,
      height = row.height,
      author = row.author,
      uploader = row.uploaderLogin.map(login => org.scalawiki.dto.User(None, Some(login))),
      year = row.year,
      date = row.date,
      monumentIds = if (row.monumentIds.isEmpty) Seq.empty else row.monumentIds.split(",").toSeq,
      pageId = row.pageId,
      metadata = None,
      categories = if (row.categories.isEmpty) Set.empty else row.categories.split(",").toSet,
      specialNominations = if (row.specialNominations.isEmpty) Set.empty else row.specialNominations.split(",").toSet,
      mime = row.mime
    )
  }

  /**
   * Insert an Image into the database
   */
  def insert(image: Image): Long = {
    val row = toImageRow(image)
    ctx.run(query[ImageRow].insertValue(lift(row)))
  }

  /**
   * Insert multiple Images into the database
   */
  def insertBatch(images: Seq[Image]): List[Long] = {
    val rows = images.map(toImageRow)
    ctx.run(liftQuery(rows).foreach(row => query[ImageRow].insertValue(row)))
  }

  /**
   * Find an Image by title
   */
  def findByTitle(title: String): Option[Image] = {
    ctx.run(query[ImageRow].filter(_.title == lift(title))).headOption.map(fromImageRow)
  }

  /**
   * Find all Images
   */
  def findAll(): Seq[Image] = {
    ctx.run(query[ImageRow]).map(fromImageRow)
  }

  /**
   * Find Images by monument ID
   */
  def findByMonumentId(monumentId: String): Seq[Image] = {
    ctx.run(query[ImageRow].filter(row => 
      sql"${row.monumentIds} LIKE ${"%" + monumentId + "%"}".asCondition
    )).map(fromImageRow)
  }

  /**
   * Find Images by author
   */
  def findByAuthor(author: String): Seq[Image] = {
    ctx.run(query[ImageRow].filter(_.author.exists(_ == lift(author)))).map(fromImageRow)
  }

  /**
   * Update an Image
   */
  def update(image: Image): Long = {
    val row = toImageRow(image)
    ctx.run(query[ImageRow].filter(_.title == lift(row.title)).updateValue(lift(row)))
  }

  /**
   * Delete an Image by title
   */
  def delete(title: String): Long = {
    ctx.run(query[ImageRow].filter(_.title == lift(title)).delete)
  }

  /**
   * Count all Images
   */
  def count(): Long = {
    ctx.run(query[ImageRow].size)
  }

  /**
   * Create the images table if it doesn't exist
   */
  def createTable(): Unit = {
    val createTableSql = """
      CREATE TABLE IF NOT EXISTS image_row (
        title VARCHAR PRIMARY KEY,
        url VARCHAR,
        page_url VARCHAR,
        size BIGINT,
        width INTEGER,
        height INTEGER,
        author VARCHAR,
        uploader_login VARCHAR,
        year VARCHAR,
        date TIMESTAMP WITH TIME ZONE,
        monument_ids VARCHAR,
        page_id BIGINT,
        categories VARCHAR,
        special_nominations VARCHAR,
        mime VARCHAR
      )
    """
    val connection = dataSource.getConnection()
    try {
      val statement = connection.createStatement()
      try {
        statement.execute(createTableSql)
      } finally {
        statement.close()
      }
    } finally {
      connection.close()
    }
  }

  /**
   * Drop the images table
   */
  def dropTable(): Unit = {
    val connection = dataSource.getConnection()
    try {
      val statement = connection.createStatement()
      try {
        statement.execute("DROP TABLE IF EXISTS image_row")
      } finally {
        statement.close()
      }
    } finally {
      connection.close()
    }
  }
}

object ImageRepository {
  /**
   * Create a new ImageRepository with a DuckDB connection
   */
  def apply(jdbcUrl: String): ImageRepository = {
    val ds = new org.duckdb.DuckDBDataSource()
    ds.setUrl(jdbcUrl)
    new ImageRepository(ds)
  }

  /**
   * Create a new ImageRepository with an in-memory DuckDB database
   */
  def inMemory(): ImageRepository = {
    apply("jdbc:duckdb:")
  }
}
