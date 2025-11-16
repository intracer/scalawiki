package org.scalawiki.duckdb

import org.scalawiki.dto.{Image, User}
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeAfterEach
import java.time.{ZonedDateTime, ZoneOffset}

class ImageRepositorySpec extends Specification with BeforeAfterEach {

  sequential

  var repository: ImageRepository = _

  override def before: Any = {
    repository = ImageRepository.inMemory()
    repository.createTable()
  }

  override def after: Any = {
    repository.dropTable()
  }

  "ImageRepository" should {

    "insert and retrieve an image" in {
      val image = Image(
        title = "Test_Image.jpg",
        url = Some("http://example.com/test.jpg"),
        pageUrl = Some("http://example.com/page"),
        size = Some(1024L),
        width = Some(800),
        height = Some(600),
        author = Some("TestAuthor"),
        uploader = Some(User(Some(1L), Some("TestUploader"))),
        year = Some("2023"),
        monumentIds = Seq("ID123", "ID456"),
        pageId = Some(42L),
        categories = Set("Category1", "Category2"),
        specialNominations = Set("Nomination1"),
        mime = Some("image/jpeg")
      )

      repository.insert(image)

      val retrieved = repository.findByTitle("Test_Image.jpg")
      retrieved must beSome
      retrieved.get.title must beEqualTo(image.title)
      retrieved.get.url must beEqualTo(image.url)
      retrieved.get.author must beEqualTo(image.author)
      retrieved.get.uploader.flatMap(_.login) must beEqualTo(Some("TestUploader"))
      retrieved.get.monumentIds must beEqualTo(image.monumentIds)
      retrieved.get.categories must beEqualTo(image.categories)
      retrieved.get.specialNominations must beEqualTo(image.specialNominations)
    }

    "insert and retrieve multiple images" in {
      val image1 = Image(
        title = "Image1.jpg",
        author = Some("Author1"),
        monumentIds = Seq("MON1")
      )

      val image2 = Image(
        title = "Image2.jpg",
        author = Some("Author2"),
        monumentIds = Seq("MON2")
      )

      repository.insertBatch(Seq(image1, image2))

      val all = repository.findAll()
      all must have size 2
      all.map(_.title) must contain(exactly("Image1.jpg", "Image2.jpg"))
    }

    "find images by author" in {
      val image1 = Image(
        title = "Image1.jpg",
        author = Some("AuthorA")
      )

      val image2 = Image(
        title = "Image2.jpg",
        author = Some("AuthorB")
      )

      val image3 = Image(
        title = "Image3.jpg",
        author = Some("AuthorA")
      )

      repository.insertBatch(Seq(image1, image2, image3))

      val byAuthorA = repository.findByAuthor("AuthorA")
      byAuthorA must have size 2
      byAuthorA.map(_.title) must contain(exactly("Image1.jpg", "Image3.jpg"))
    }

    "find images by monument ID" in {
      val image1 = Image(
        title = "Image1.jpg",
        monumentIds = Seq("MON123")
      )

      val image2 = Image(
        title = "Image2.jpg",
        monumentIds = Seq("MON456")
      )

      val image3 = Image(
        title = "Image3.jpg",
        monumentIds = Seq("MON123", "MON789")
      )

      repository.insertBatch(Seq(image1, image2, image3))

      val byMonument = repository.findByMonumentId("MON123")
      byMonument must have size 2
      byMonument.map(_.title) must contain(exactly("Image1.jpg", "Image3.jpg"))
    }

    "update an existing image" in {
      val image = Image(
        title = "UpdateTest.jpg",
        author = Some("OriginalAuthor")
      )

      repository.insert(image)

      val updatedImage = image.copy(author = Some("UpdatedAuthor"))
      repository.update(updatedImage)

      val retrieved = repository.findByTitle("UpdateTest.jpg")
      retrieved must beSome
      retrieved.get.author must beEqualTo(Some("UpdatedAuthor"))
    }

    "delete an image" in {
      val image = Image(
        title = "DeleteTest.jpg",
        author = Some("TestAuthor")
      )

      repository.insert(image)
      repository.findByTitle("DeleteTest.jpg") must beSome

      repository.delete("DeleteTest.jpg")
      repository.findByTitle("DeleteTest.jpg") must beNone
    }

    "count images" in {
      val image1 = Image(title = "Image1.jpg")
      val image2 = Image(title = "Image2.jpg")
      val image3 = Image(title = "Image3.jpg")

      repository.insertBatch(Seq(image1, image2, image3))

      repository.count() must beEqualTo(3L)
    }

    "handle images with dates" in {
      val date = ZonedDateTime.of(2023, 6, 15, 12, 30, 0, 0, ZoneOffset.UTC)
      val image = Image(
        title = "DateTest.jpg",
        date = Some(date)
      )

      repository.insert(image)

      val retrieved = repository.findByTitle("DateTest.jpg")
      retrieved must beSome
      // Note: DuckDB timestamp comparison may have precision differences
      retrieved.get.date.map(_.toEpochSecond) must beEqualTo(Some(date.toEpochSecond))
    }

    "handle empty monument IDs and categories" in {
      val image = Image(
        title = "EmptyTest.jpg",
        monumentIds = Seq.empty,
        categories = Set.empty,
        specialNominations = Set.empty
      )

      repository.insert(image)

      val retrieved = repository.findByTitle("EmptyTest.jpg")
      retrieved must beSome
      retrieved.get.monumentIds must beEmpty
      retrieved.get.categories must beEmpty
      retrieved.get.specialNominations must beEmpty
    }

    "return empty result when image not found" in {
      repository.findByTitle("NonExistent.jpg") must beNone
    }

    "return empty list when no images match criteria" in {
      val image = Image(
        title = "Test.jpg",
        author = Some("AuthorA")
      )
      repository.insert(image)

      repository.findByAuthor("NonExistentAuthor") must beEmpty
      repository.findByMonumentId("NonExistentMonument") must beEmpty
    }
  }
}
