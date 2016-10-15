package org.scalawiki.wlx.stat

import org.scalawiki.dto.Image
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.dto._
import org.scalawiki.wlx.dto.lists.ListConfig._
import org.scalawiki.wlx.{ImageDB, MonumentDB}
import org.specs2.mutable.Specification

class AuthorsMonumentsSpec extends Specification {

  val contest = new Contest(ContestType.WLE, Country.Ukraine, 2013, uploadConfigs = Seq.empty[UploadConfig])

  def getTable(
                images: Seq[Image],
                monuments: Seq[Monument],
                contest: Contest = contest,
                gallery: Boolean = false): Table = {
    val mdb = Some(new MonumentDB(contest, monuments))

    val db = new ImageDB(contest, images, mdb, mdb)

    new AuthorMonuments(db, gallery = gallery).table
  }

  def monument(id: String, name: String) =
    new Monument(id = id, name = name, listConfig = Some(WlmUa))

  def monuments(n: Int, regionId: String, namePrefix: String, startId: Int = 1): Seq[Monument] =
    (startId until startId + n).map(i => monument(s"$regionId-xxx-000$i", namePrefix + i))


  "authorsMonumentsTable" should {
    "be empty without regions" in {
      val noRegions = contest.copy(country = Country.Azerbaijan)
      val table = getTable(Seq.empty[Image], Seq.empty[Monument], noRegions)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded")

      table.data === Seq(
        Seq("Total") ++ Seq.fill(2)("0")
      )
    }

    "work without old monument db" in {
      val noRegions = contest.copy(country = Country.Azerbaijan)

      val mdb = Some(new MonumentDB(contest, Seq.empty[Monument]))

      val db = new ImageDB(noRegions, Seq.empty[Image], mdb, None)

      val table = new AuthorMonuments(db).table

      table.headers === Seq("User", "Objects pictured", "Photos uploaded")

      table.data === Seq(
        Seq("Total") ++ Seq.fill(2)("0")
      )
    }

    "be empty with regions" in {
      val images = Seq.empty[Image]
      val monuments = Seq.empty[Monument]
      val table = getTable(images, monuments)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded") ++ contest.country.regionNames

      table.data === Seq(
        Seq("Total") ++ Seq.fill(2 + contest.country.regions.size)("0")
      )
    }

    "have 1 unknown image" in {
      val images = Seq(Image("image1.jpg"))
      val monuments = Seq.empty[Monument]
      val table = getTable(images, monuments)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded") ++ contest.country.regionNames

      table.data === Seq(
        Seq("Total", "0", "1") ++ Seq.fill(contest.country.regions.size)("0")
      )
    }

    "have 1 image with author" in {
      val user = "user"
      val images = Seq(Image("image1.jpg", author = Some(user)))
      val monuments = Seq.empty[Monument]
      val table: Table = getTable(images, monuments)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded") ++ contest.country.regionNames

      // TODO why no author???
      table.data === Seq(
        Seq("Total", "0", "1") ++ Seq.fill(contest.country.regions.size)("0")
      )
    }

    "have 1 image with author and monument with undefined regions" in {
      val user = "user"
      val images = Seq(Image("image1.jpg", author = Some(user), monumentId = Some("123")))
      val monuments = Seq(new Monument(id = "123", name = "123 monument"))

      val table = getTable(images, monuments)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded") ++ contest.country.regionNames

      table.data === Seq(
        Seq("Total", "1", "1") ++ Seq.fill(contest.country.regions.size)("0"),
        Seq("[[User:user|user]]", "1", "1") ++ Seq.fill(contest.country.regions.size)("0")
      )
    }

    "have 1 image with author and monument no regions" in {
      val noRegions = contest.copy(country = Country.Azerbaijan)
      val user = "user"
      val images = Seq(Image("image1.jpg", author = Some(user), monumentId = Some("123")))
      val monuments = Seq(new Monument(id = "123", name = "123 monument"))

      val table = getTable(images, monuments, noRegions)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded")

      table.data === Seq(
        Seq("Total", "1", "1"),
        Seq("[[User:user|user]]", "1", "1")
      )
    }

    "link to user details" in {
      val noRegions = contest.copy(country = Country.Azerbaijan)
      val user = "user"
      val images = Seq(Image("image1.jpg", author = Some(user), monumentId = Some("123")))
      val monuments = Seq(new Monument(id = "123", name = "123 monument"))

      val table = getTable(images, monuments, noRegions, gallery = true)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded")

      table.data === Seq(
        Seq("Total", "1", "1"),
        Seq("[[User:user|user]]", "[[Commons:Wiki Loves Earth 2013 in Azerbaijan/user|1]]", "1")
      )
    }

    "count user's images and monuments" in {
      val noRegions = contest.copy(country = Country.Azerbaijan)
      val (user1, user2) = ("user1", "user2")
      val images = Seq(
        Image("image11.jpg", author = Some(user1), monumentId = Some("11")),
        Image("image12.jpg", author = Some(user1), monumentId = Some("11")),
        Image("image13.jpg", author = Some(user1), monumentId = Some("12")),

        Image("image21.jpg", author = Some(user2), monumentId = Some("21")),
        Image("image22.jpg", author = Some(user2), monumentId = Some("22")),
        Image("image23.jpg", author = Some(user2), monumentId = Some("22")),
        Image("image24.jpg", author = Some(user2), monumentId = Some("23")),
        Image("image25.jpg", author = Some(user2), monumentId = Some("24"))
      )

      val monuments = Seq(
        Monument(id = "11", name = "11 m"), Monument(id = "12", name = "12 m"),

        Monument(id = "21", name = "21 m"), Monument(id = "22", name = "22 m"),
        Monument(id = "23", name = "23 m"), Monument(id = "24", name = "24 m")
      )

      val table = getTable(images, monuments, noRegions)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded")

      table.data === Seq(
        Seq("Total", "6", "8"),
        Seq("[[User:user2|user2]]", "4", "5"),
        Seq("[[User:user1|user1]]", "2", "3")
      )
    }

    "with regions in" in {
      val images2 = Seq(
        Image("File:Img11y2f1.jpg", monumentId = Some("01-xxx-0001"), author = Some("FromCrimeaOld")),
        Image("File:Img12y2f1.jpg", monumentId = Some("01-xxx-0002"), author = Some("FromCrimeaNew")),
        Image("File:Img52y2f1.jpg", monumentId = Some("05-xxx-0002"), author = Some("FromPodillyaNew1")),
        Image("File:Img52y2f2.jpg", monumentId = Some("05-xxx-0002"), author = Some("FromPodillyaNew2")),
        Image("File:Img72y2f1.jpg", monumentId = Some("07-xxx-0002"), author = Some("FromVolynNew"))
      )

      val mDb = new MonumentDB(contest,
        monuments(2, "01", "Crimea") ++
          monuments(5, "05", "Podillya") ++
          monuments(7, "07", "Volyn")
      )

      val db = new ImageDB(contest, images2, Some(mDb), Some(mDb))

      val table = new AuthorMonuments(db).table
      val data = table.data

      data.size === 6

      table.headers.slice(0, 6) === Seq("User", "Objects pictured", "Photos uploaded", "Автономна Республіка Крим", "Вінницька область", "Волинська область")

      data.head === Seq("Total", "4", "5", "2", "1", "1") ++ Seq.fill(24)("0")


      data.slice(1, 6) ===
        Seq(
          Seq("[[User:FromCrimeaNew|FromCrimeaNew]]", "1", "1", "1", "0", "0") ++ Seq.fill(24)("0"),
          Seq("[[User:FromCrimeaOld|FromCrimeaOld]]", "1", "1", "1", "0", "0") ++ Seq.fill(24)("0"),
          Seq("[[User:FromPodillyaNew1|FromPodillyaNew1]]", "1", "1", "0", "1", "0") ++ Seq.fill(24)("0"),
          Seq("[[User:FromPodillyaNew2|FromPodillyaNew2]]", "1", "1", "0", "1", "0") ++ Seq.fill(24)("0"),
          Seq("[[User:FromVolynNew|FromVolynNew]]", "1", "1", "0", "0", "1") ++ Seq.fill(24)("0")
        )
    }

    "rate no new images" in {
      val images2 = Seq(
        Image("File:Img11y2f1.jpg", monumentId = Some("01-xxx-0001"), author = Some("FromCrimeaOld")),
        Image("File:Img12y2f1.jpg", monumentId = Some("01-xxx-0002"), author = Some("FromCrimeaNew")),
        Image("File:Img52y2f1.jpg", monumentId = Some("05-xxx-0002"), author = Some("FromPodillyaNew1")),
        Image("File:Img52y2f2.jpg", monumentId = Some("05-xxx-0002"), author = Some("FromPodillyaNew2")),
        Image("File:Img72y2f1.jpg", monumentId = Some("07-xxx-0002"), author = Some("FromVolynNew"))
      )

      val mDb = new MonumentDB(contest,
        monuments(2, "01", "Crimea") ++
          monuments(5, "05", "Podillya") ++
          monuments(7, "07", "Volyn")
      )

      val db = new ImageDB(contest, images2, Some(mDb), Some(mDb))

      val table = new AuthorMonuments(db, rating = true).table
      val data = table.data

      data.size === 6

      table.headers.slice(0, 9) === Seq("User", "Objects pictured", "Existing", "New", "Rating", "Photos uploaded", "Автономна Республіка Крим", "Вінницька область", "Волинська область")

      data.head === Seq("Total", "4", "4", "0", "4",  "5", "2", "1", "1") ++ Seq.fill(24)("0")


      data.slice(1, 6) ===
        Seq(
          Seq("[[User:FromCrimeaNew|FromCrimeaNew]]", "1", "1", "0", "1", "1", "1", "0", "0") ++ Seq.fill(24)("0"),
          Seq("[[User:FromCrimeaOld|FromCrimeaOld]]", "1", "1", "0", "1", "1", "1", "0", "0") ++ Seq.fill(24)("0"),
          Seq("[[User:FromPodillyaNew1|FromPodillyaNew1]]", "1", "1", "0", "1", "1", "0", "1", "0") ++ Seq.fill(24)("0"),
          Seq("[[User:FromPodillyaNew2|FromPodillyaNew2]]", "1", "1", "0", "1", "1", "0", "1", "0") ++ Seq.fill(24)("0"),
          Seq("[[User:FromVolynNew|FromVolynNew]]", "1", "1", "0", "1", "1", "0", "0", "1") ++ Seq.fill(24)("0")
        )
    }

    "rate with all new images" in {
      val output = new AuthorsStat

      val images2 = Seq(
        Image("File:Img11y2f1.jpg", monumentId = Some("01-xxx-0001"), author = Some("FromCrimeaOld")),
        Image("File:Img12y2f1.jpg", monumentId = Some("01-xxx-0002"), author = Some("FromCrimeaNew")),
        Image("File:Img52y2f1.jpg", monumentId = Some("05-xxx-0002"), author = Some("FromPodillyaNew1")),
        Image("File:Img52y2f2.jpg", monumentId = Some("05-xxx-0002"), author = Some("FromPodillyaNew2")),
        Image("File:Img72y2f1.jpg", monumentId = Some("07-xxx-0002"), author = Some("FromVolynNew"))
      )

      val mDb = new MonumentDB(contest,
        monuments(2, "01", "Crimea") ++
          monuments(5, "05", "Podillya") ++
          monuments(7, "07", "Volyn")
      )

      val db = new ImageDB(contest, images2, Some(mDb), Some(new MonumentDB(contest, Seq.empty)))

      val table = new AuthorMonuments(db, rating = true).table
      val data = table.data

      data.size === 6

      table.headers.slice(0, 9) === Seq("User", "Objects pictured", "Existing", "New", "Rating", "Photos uploaded", "Автономна Республіка Крим", "Вінницька область", "Волинська область")

      data.head === Seq("Total", "4", "0", "4", "8",  "5", "2", "1", "1") ++ Seq.fill(24)("0")


      data.slice(1, 6) ===
        Seq(
          Seq("[[User:FromCrimeaNew|FromCrimeaNew]]", "1", "0", "1", "2", "1", "2", "0", "0") ++ Seq.fill(24)("0"),
          Seq("[[User:FromCrimeaOld|FromCrimeaOld]]", "1", "0", "1", "2", "1", "2", "0", "0") ++ Seq.fill(24)("0"),
          Seq("[[User:FromPodillyaNew1|FromPodillyaNew1]]", "1", "0", "1", "2", "1", "0", "2", "0") ++ Seq.fill(24)("0"),
          Seq("[[User:FromPodillyaNew2|FromPodillyaNew2]]", "1", "0", "1", "2", "1", "0", "2", "0") ++ Seq.fill(24)("0"),
          Seq("[[User:FromVolynNew|FromVolynNew]]", "1", "0", "1", "2", "1", "0", "0", "2") ++ Seq.fill(24)("0")
        )
    }

    "order by rate in" in {
      val images1 = Seq(
        Image("File:Img11y1f1.jpg", monumentId = Some("01-xxx-0001"), author = Some("FromCrimea")),
        Image("File:Img51y1f1.jpg", monumentId = Some("05-xxx-0001"), author = Some("FromPodillya")),
        Image("File:Img71y1f1.jpg", monumentId = Some("07-xxx-0001"), author = Some("FromVolyn"))
      )

      val images2 = Seq(
        Image("File:Img11y2f1.jpg", monumentId = Some("01-xxx-0001"), author = Some("FromCrimeaOld")),
        Image("File:Img12y2f1.jpg", monumentId = Some("01-xxx-0002"), author = Some("FromCrimeaNew")),
        Image("File:Img52y2f1.jpg", monumentId = Some("05-xxx-0002"), author = Some("FromPodillyaNew1")),
        Image("File:Img52y2f2.jpg", monumentId = Some("05-xxx-0002"), author = Some("FromPodillyaNew2")),
        Image("File:Img72y2f1.jpg", monumentId = Some("07-xxx-0002"), author = Some("FromVolynNew"))
      )

      val mDb = new MonumentDB(contest,
        monuments(2, "01", "Crimea") ++
          monuments(5, "05", "Podillya") ++
          monuments(7, "07", "Volyn")
      )

      val oldIds = images1.flatMap(_.monumentId).toSet
      val oldMdb = new MonumentDB(contest, mDb.monuments.filter(m => oldIds.contains(m.id)))

      val db = new ImageDB(contest, images2, Some(mDb), Some(oldMdb))

      val table = new AuthorMonuments(db, rating = true).table
      val data = table.data

      data.size === 6

      table.headers.slice(0, 9) === Seq("User", "Objects pictured",  "Existing", "New", "Rating", "Photos uploaded", "Автономна Республіка Крим", "Вінницька область", "Волинська область")

      data.head === Seq("Total", "4", "1", "3", "7",  "5", "2", "1", "1") ++ Seq.fill(24)("0")


      data.slice(1, 9) ===
        Seq(
          Seq("[[User:FromCrimeaNew|FromCrimeaNew]]", "1", "0", "1", "2", "1", "2", "0", "0") ++ Seq.fill(24)("0"),
          Seq("[[User:FromPodillyaNew1|FromPodillyaNew1]]", "1", "0", "1", "2", "1", "0", "2", "0") ++ Seq.fill(24)("0"),
          Seq("[[User:FromPodillyaNew2|FromPodillyaNew2]]", "1", "0", "1", "2", "1", "0", "2", "0") ++ Seq.fill(24)("0"),
          Seq("[[User:FromVolynNew|FromVolynNew]]", "1", "0", "1", "2", "1", "0", "0", "2") ++ Seq.fill(24)("0"),
          Seq("[[User:FromCrimeaOld|FromCrimeaOld]]", "1", "1", "0", "1", "1", "1", "0", "0") ++ Seq.fill(24)("0")
        )
    }
  }
}