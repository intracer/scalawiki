package org.scalawiki.wlx.stat

import org.scalawiki.dto.Image
import org.scalawiki.dto.markup.Table
import org.scalawiki.wlx.dto._
import org.scalawiki.wlx.dto.lists.ListConfig._
import org.scalawiki.wlx.stat.reports.AuthorMonuments
import org.scalawiki.wlx.{ImageDB, MonumentDB}
import org.specs2.mutable.Specification

class AuthorsMonumentsSpec extends Specification {

  val contest = new Contest(
    ContestType.WLE,
    Country.Ukraine,
    2013,
    uploadConfigs = Seq.empty[UploadConfig]
  )

  def getTable(
      images: Seq[Image],
      monuments: Seq[Monument],
      contest: Contest = contest,
      gallery: Boolean = false
  ): Table = {
    val mdb = Some(new MonumentDB(contest, monuments))

    val db = new ImageDB(contest, images, mdb)
    val contestStat = ContestStat(contest, 2013, mdb, Some(db), Some(db))
    new AuthorMonuments(contestStat, gallery = gallery).table
  }

  def monument(id: String, name: String) =
    new Monument(id = id, name = name, listConfig = Some(WlmUa))

  def monuments(
      n: Int,
      regionId: String,
      namePrefix: String,
      startId: Int = 1
  ): Seq[Monument] =
    (startId until startId + n).map(i =>
      monument(s"$regionId-xxx-000$i", namePrefix + i)
    )

  def image(
      i: Int,
      author: Option[String] = None,
      monumentId: Option[String] = None
  ): Image = {
    Image(
      s"Image$i.jpg",
      author = author,
      monumentIds = monumentId.toList,
      pageId = Some(i),
      width = Some(800),
      height = Some(600)
    )
  }

  "authorsMonumentsTable" should {
    "be empty without regions" in {
      val noRegions = contest.copy(country = Country.Azerbaijan)
      val table = getTable(Seq.empty[Image], Seq.empty[Monument], noRegions)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded")

      table.data === Seq(
        Seq("Total", "0.0", "0")
      )
    }

    "work without old monument db" in {
      val noRegions = contest.copy(country = Country.Azerbaijan)

      val mdb = Some(new MonumentDB(contest, Seq.empty[Monument]))

      val db = new ImageDB(noRegions, Seq.empty[Image], mdb)

      val contestStat = ContestStat(contest, 2013, mdb, Some(db), Some(db))
      val table = new AuthorMonuments(contestStat).table

      table.headers === Seq(
        "User",
        "Objects pictured",
        "Photos uploaded"
      ) ++ contest.country.regionNames

      table.data === Seq(
        Seq("Total", "0.0", "0") ++ Seq.fill(27)("0.0")
      )
    }

    "be empty with regions" in {
      val images = Seq.empty[Image]
      val monuments = Seq.empty[Monument]
      val table = getTable(images, monuments)

      table.headers === Seq(
        "User",
        "Objects pictured",
        "Photos uploaded"
      ) ++ contest.country.regionNames

      table.data === Seq(
        Seq("Total", "0.0", "0") ++ Seq
          .fill(contest.country.regions.size)("0.0")
      )
    }

    "have 1 unknown image" in {
      val images = Seq(Image("image1.jpg", pageId = Some(1)))
      val monuments = Seq.empty[Monument]
      val table = getTable(images, monuments)

      table.headers === Seq(
        "User",
        "Objects pictured",
        "Photos uploaded"
      ) ++ contest.country.regionNames

      table.data === Seq(
        Seq("Total", "0.0", "1") ++ Seq
          .fill(contest.country.regions.size)("0.0")
      )
    }

    "have 1 image with author and no monument" in {
      val user = "user"
      val images = Seq(image(1, Some(user), None))
      val monuments = Seq.empty[Monument]
      val table: Table = getTable(images, monuments)

      table.headers === Seq(
        "User",
        "Objects pictured",
        "Photos uploaded"
      ) ++ contest.country.regionNames

      table.data === Seq(
        Seq("Total", "0.0", "1") ++ Seq
          .fill(contest.country.regions.size)("0.0")
      )
    }

    "have 1 image with author and monument with undefined regions" in {
      val user = "user"
      val images = Seq(image(1, Some(user), Some("123")))
      val monuments = Seq(new Monument(id = "123", name = "123 monument"))

      val table = getTable(images, monuments)

      table.headers === Seq(
        "User",
        "Objects pictured",
        "Photos uploaded"
      ) ++ contest.country.regionNames

      val data = table.data
      data.head === Seq("Total", "1.0", "1") ++ Seq.fill(
        contest.country.regions.size
      )("0.0")
      data.size === 2
      data.last === Seq("[[User:user|user]]", "1.0", "1") ++ Seq.fill(
        contest.country.regions.size
      )("0.0")
    }

    "have 1 image with author and monument no regions" in {
      val noRegions = contest.copy(country = Country.Azerbaijan)
      val user = "user"
      val images = Seq(
        Image(
          "image1.jpg",
          author = Some(user),
          monumentIds = List("123"),
          pageId = Some(1)
        )
      )
      val monuments = Seq(new Monument(id = "123", name = "123 monument"))

      val table = getTable(images, monuments, noRegions)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded")

      table.data === Seq(
        Seq("Total", "1.0", "1"),
        Seq("[[User:user|user]]", "1.0", "1")
      )
    }

    "link to user details" in {
      val noRegions = contest.copy(country = Country.Azerbaijan)
      val user = "user"
      val images = Seq(
        Image(
          "image1.jpg",
          author = Some(user),
          monumentIds = List("123"),
          pageId = Some(1)
        )
      )
      val monuments = Seq(new Monument(id = "123", name = "123 monument"))

      val table = getTable(images, monuments, noRegions, gallery = true)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded")

      table.data === Seq(
        Seq("Total", "1.0", "1"),
        Seq(
          "[[User:user|user]]",
          "[[Commons:Wiki Loves Earth 2013 in Azerbaijan/user|1.0]]",
          "1"
        )
      )
    }

    "count user's images and monuments" in {
      val noRegions = contest.copy(country = Country.Azerbaijan)
      val (user1, user2) = ("user1", "user2")
      val images = Seq(
        Image(
          "image11.jpg",
          author = Some(user1),
          monumentIds = List("11"),
          pageId = Some(111)
        ),
        Image(
          "image12.jpg",
          author = Some(user1),
          monumentIds = List("11"),
          pageId = Some(112)
        ),
        Image(
          "image13.jpg",
          author = Some(user1),
          monumentIds = List("12"),
          pageId = Some(113)
        ),
        Image(
          "image21.jpg",
          author = Some(user2),
          monumentIds = List("21"),
          pageId = Some(121)
        ),
        Image(
          "image22.jpg",
          author = Some(user2),
          monumentIds = List("22"),
          pageId = Some(122)
        ),
        Image(
          "image23.jpg",
          author = Some(user2),
          monumentIds = List("22"),
          pageId = Some(123)
        ),
        Image(
          "image24.jpg",
          author = Some(user2),
          monumentIds = List("23"),
          pageId = Some(124)
        ),
        Image(
          "image25.jpg",
          author = Some(user2),
          monumentIds = List("24"),
          pageId = Some(125)
        )
      )

      val monuments = Seq(
        Monument(id = "11", name = "11 m"),
        Monument(id = "12", name = "12 m"),
        Monument(id = "21", name = "21 m"),
        Monument(id = "22", name = "22 m"),
        Monument(id = "23", name = "23 m"),
        Monument(id = "24", name = "24 m")
      )

      val table = getTable(images, monuments, noRegions)

      table.headers === Seq("User", "Objects pictured", "Photos uploaded")

      table.data === Seq(
        Seq("Total", "6.0", "8"),
        Seq("[[User:user2|user2]]", "4.0", "5"),
        Seq("[[User:user1|user1]]", "2.0", "3")
      )
    }

    "with regions in" in {
      val images2 = Seq(
        Image(
          "File:Img11y2f1.jpg",
          monumentIds = List("01-xxx-0001"),
          author = Some("FromCrimeaOld"),
          pageId = Some(11)
        ),
        Image(
          "File:Img12y2f1.jpg",
          monumentIds = List("01-xxx-0002"),
          author = Some("FromCrimeaNew"),
          pageId = Some(12)
        ),
        Image(
          "File:Img52y2f1.jpg",
          monumentIds = List("05-xxx-0002"),
          author = Some("FromPodillyaNew1"),
          pageId = Some(5121)
        ),
        Image(
          "File:Img52y2f2.jpg",
          monumentIds = List("05-xxx-0002"),
          author = Some("FromPodillyaNew2"),
          pageId = Some(5222)
        ),
        Image(
          "File:Img72y2f1.jpg",
          monumentIds = List("07-xxx-0002"),
          author = Some("FromVolynNew"),
          pageId = Some(72)
        )
      )

      val mDb = new MonumentDB(
        contest,
        monuments(2, "01", "Crimea") ++
          monuments(5, "05", "Podillya") ++
          monuments(7, "07", "Volyn")
      )

      val db = new ImageDB(contest, images2, Some(mDb))
      val contestStat =
        new ContestStat(contest, 2013, Some(mDb), Some(db), Some(db))
      val table = new AuthorMonuments(contestStat).table
      val data = table.data

      data.size === 6

      table.headers.slice(0, 6) === Seq(
        "User",
        "Objects pictured",
        "Photos uploaded",
        "Автономна Республіка Крим",
        "Вінницька область",
        "Волинська область"
      )

      data.head === Seq("Total", "4.0", "5", "2.0", "1.0", "1.0") ++ Seq.fill(
        24
      )("0.0")

      data.slice(1, 6) ===
        Seq(
          Seq(
            "[[User:FromCrimeaNew|FromCrimeaNew]]",
            "1.0",
            "1",
            "1.0",
            "0.0",
            "0.0"
          ) ++ Seq
            .fill(24)("0.0"),
          Seq(
            "[[User:FromCrimeaOld|FromCrimeaOld]]",
            "1.0",
            "1",
            "1.0",
            "0.0",
            "0.0"
          ) ++ Seq
            .fill(24)("0.0"),
          Seq(
            "[[User:FromPodillyaNew1|FromPodillyaNew1]]",
            "1.0",
            "1",
            "0.0",
            "1.0",
            "0.0"
          ) ++ Seq.fill(24)("0.0"),
          Seq(
            "[[User:FromPodillyaNew2|FromPodillyaNew2]]",
            "1.0",
            "1",
            "0.0",
            "1.0",
            "0.0"
          ) ++ Seq.fill(24)("0.0"),
          Seq(
            "[[User:FromVolynNew|FromVolynNew]]",
            "1.0",
            "1",
            "0.0",
            "0.0",
            "1.0"
          ) ++ Seq
            .fill(24)("0.0")
        )
    }

    "rate no new images" in {
      val images2 = Seq(
        Image(
          "File:Img11y2f1.jpg",
          monumentIds = List("01-xxx-0001"),
          author = Some("FromCrimeaOld"),
          pageId = Some(11)
        ),
        Image(
          "File:Img12y2f1.jpg",
          monumentIds = List("01-xxx-0002"),
          author = Some("FromCrimeaNew"),
          pageId = Some(12)
        ),
        Image(
          "File:Img52y2f1.jpg",
          monumentIds = List("05-xxx-0002"),
          author = Some("FromPodillyaNew1"),
          pageId = Some(5221)
        ),
        Image(
          "File:Img52y2f2.jpg",
          monumentIds = List("05-xxx-0002"),
          author = Some("FromPodillyaNew2"),
          pageId = Some(5222)
        ),
        Image(
          "File:Img72y2f1.jpg",
          monumentIds = List("07-xxx-0002"),
          author = Some("FromVolynNew"),
          pageId = Some(72)
        )
      )

      val mDb = new MonumentDB(
        contest,
        monuments(2, "01", "Crimea") ++
          monuments(5, "05", "Podillya") ++
          monuments(7, "07", "Volyn")
      )

      val images1 = images2.map { i2 =>
        i2.copy(
          pageId = i2.pageId.map(_ + 100),
          title = i2.title.replace("Img", "Img0")
        )

      }
      val db = new ImageDB(contest, images2, Some(mDb))
      val totalDb = new ImageDB(contest, images1 ++ images2, Some(mDb))

      val contestStat = new ContestStat(
        contest.copy(rateConfig = RateConfig(newObjectRating = Some(3))),
        2013,
        Some(mDb),
        Some(db),
        Some(totalDb)
      )
      val table = new AuthorMonuments(contestStat).table
      val data = table.data

      data.size === 6

      table.headers.slice(0, 10) === Seq(
        "User",
        "Objects pictured",
        "Existing",
        "New for author",
        "New",
        "Rating",
        "Photos uploaded",
        "Автономна Республіка Крим",
        "Вінницька область",
        "Волинська область"
      )

      data.head === Seq(
        "Total",
        "4.0",
        "0",
        "4",
        "0",
        "4",
        "5",
        "2.0",
        "1.0",
        "1.0"
      ) ++ Seq
        .fill(24)("0.0")

      data.slice(1, 6) ===
        Seq(
          Seq(
            "[[User:FromCrimeaNew|FromCrimeaNew]]",
            "1.0",
            "1",
            "0",
            "0",
            "1.0",
            "1",
            "1.0",
            "0.0",
            "0.0"
          ) ++ Seq.fill(24)("0.0"),
          Seq(
            "[[User:FromCrimeaOld|FromCrimeaOld]]",
            "1.0",
            "1",
            "0",
            "0",
            "1.0",
            "1",
            "1.0",
            "0.0",
            "0.0"
          ) ++ Seq.fill(24)("0.0"),
          Seq(
            "[[User:FromPodillyaNew1|FromPodillyaNew1]]",
            "1.0",
            "1",
            "0",
            "0",
            "1.0",
            "1",
            "0.0",
            "1.0",
            "0.0"
          ) ++ Seq.fill(24)("0.0"),
          Seq(
            "[[User:FromPodillyaNew2|FromPodillyaNew2]]",
            "1.0",
            "1",
            "0",
            "0",
            "1.0",
            "1",
            "0.0",
            "1.0",
            "0.0"
          ) ++ Seq.fill(24)("0.0"),
          Seq(
            "[[User:FromVolynNew|FromVolynNew]]",
            "1.0",
            "1",
            "0",
            "0",
            "1.0",
            "1",
            "0.0",
            "0.0",
            "1.0"
          ) ++ Seq.fill(24)("0.0")
        )
    }

    "rate with all new images" in {
      val images2 = Seq(
        Image(
          "File:Img11y2f1.jpg",
          monumentIds = List("01-xxx-0001"),
          author = Some("FromCrimeaOld"),
          pageId = Some(11)
        ),
        Image(
          "File:Img12y2f1.jpg",
          monumentIds = List("01-xxx-0002"),
          author = Some("FromCrimeaNew"),
          pageId = Some(12)
        ),
        Image(
          "File:Img52y2f1.jpg",
          monumentIds = List("05-xxx-0002"),
          author = Some("FromPodillyaNew1"),
          pageId = Some(5221)
        ),
        Image(
          "File:Img52y2f2.jpg",
          monumentIds = List("05-xxx-0002"),
          author = Some("FromPodillyaNew2"),
          pageId = Some(5222)
        ),
        Image(
          "File:Img72y2f1.jpg",
          monumentIds = List("07-xxx-0002"),
          author = Some("FromVolynNew"),
          pageId = Some(72)
        )
      )

      val mDb = new MonumentDB(
        contest,
        monuments(2, "01", "Crimea") ++
          monuments(5, "05", "Podillya") ++
          monuments(7, "07", "Volyn")
      )

      val db = new ImageDB(contest, images2, Some(mDb))

      val contestStat = new ContestStat(
        contest.copy(rateConfig = RateConfig(newObjectRating = Some(3))),
        2013,
        Some(mDb),
        Some(db),
        Some(db)
      )

      val table = new AuthorMonuments(contestStat).table
      val data = table.data

      data.size === 6

      table.headers.slice(0, 10) === Seq(
        "User",
        "Objects pictured",
        "Existing",
        "New for author",
        "New",
        "Rating",
        "Photos uploaded",
        "Автономна Республіка Крим",
        "Вінницька область",
        "Волинська область"
      )

      data.head === Seq(
        "Total",
        "4.0",
        "0",
        "0",
        "4",
        "4",
        "5",
        "2.0",
        "1.0",
        "1.0"
      ) ++ Seq
        .fill(24)("0.0")

      data.slice(1, 6) ===
        Seq(
          Seq(
            "[[User:FromCrimeaNew|FromCrimeaNew]]",
            "1.0",
            "0",
            "0",
            "1",
            "3.0",
            "1",
            "3.0",
            "0.0",
            "0.0"
          ) ++ Seq.fill(24)("0.0"),
          Seq(
            "[[User:FromCrimeaOld|FromCrimeaOld]]",
            "1.0",
            "0",
            "0",
            "1",
            "3.0",
            "1",
            "3.0",
            "0.0",
            "0.0"
          ) ++ Seq.fill(24)("0.0"),
          Seq(
            "[[User:FromPodillyaNew1|FromPodillyaNew1]]",
            "1.0",
            "0",
            "0",
            "1",
            "3.0",
            "1",
            "0.0",
            "3.0",
            "0.0"
          ) ++ Seq.fill(24)("0.0"),
          Seq(
            "[[User:FromPodillyaNew2|FromPodillyaNew2]]",
            "1.0",
            "0",
            "0",
            "1",
            "3.0",
            "1",
            "0.0",
            "3.0",
            "0.0"
          ) ++ Seq.fill(24)("0.0"),
          Seq(
            "[[User:FromVolynNew|FromVolynNew]]",
            "1.0",
            "0",
            "0",
            "1",
            "3.0",
            "1",
            "0.0",
            "0.0",
            "3.0"
          ) ++ Seq.fill(24)("0.0")
        )
    }

    "order by rate in" in {
      val images1 = Seq(
        Image(
          "File:Img11y1f1.jpg",
          monumentIds = List("01-xxx-0001"),
          author = Some("FromCrimea"),
          pageId = Some(1111)
        ),
        Image(
          "File:Img51y1f1.jpg",
          monumentIds = List("05-xxx-0001"),
          author = Some("FromPodillya"),
          pageId = Some(51)
        ),
        Image(
          "File:Img71y1f1.jpg",
          monumentIds = List("07-xxx-0001"),
          author = Some("FromVolyn"),
          pageId = Some(71)
        )
      )

      val images2 = Seq(
        Image(
          "File:Img11y2f1.jpg",
          monumentIds = List("01-xxx-0001"),
          author = Some("FromCrimeaOld"),
          pageId = Some(1121)
        ),
        Image(
          "File:Img12y2f1.jpg",
          monumentIds = List("01-xxx-0002"),
          author = Some("FromCrimeaNew"),
          pageId = Some(1221)
        ),
        Image(
          "File:Img52y2f1.jpg",
          monumentIds = List("05-xxx-0002"),
          author = Some("FromPodillyaNew1"),
          pageId = Some(5221)
        ),
        Image(
          "File:Img52y2f2.jpg",
          monumentIds = List("05-xxx-0002"),
          author = Some("FromPodillyaNew2"),
          pageId = Some(5222)
        ),
        Image(
          "File:Img72y2f1.jpg",
          monumentIds = List("07-xxx-0002"),
          author = Some("FromVolynNew"),
          pageId = Some(72)
        )
      )

      val mDb = new MonumentDB(
        contest,
        monuments(2, "01", "Crimea") ++
          monuments(5, "05", "Podillya") ++
          monuments(7, "07", "Volyn")
      )

      val oldIds = images1.flatMap(_.monumentId).toSet
      val db = new ImageDB(contest, images2, Some(mDb))
      val totalDb = new ImageDB(contest, images1 ++ images2, Some(mDb))

      val contestStat = new ContestStat(
        contest.copy(rateConfig = RateConfig(newObjectRating = Some(3))),
        2013,
        Some(mDb),
        Some(db),
        Some(totalDb)
      )

      val table = new AuthorMonuments(contestStat).table
      val data = table.data

      data.size === 6

      table.headers.slice(0, 10) === Seq(
        "User",
        "Objects pictured",
        "Existing",
        "New for author",
        "New",
        "Rating",
        "Photos uploaded",
        "Автономна Республіка Крим",
        "Вінницька область",
        "Волинська область"
      )

      data.head === Seq(
        "Total",
        "4.0",
        "0",
        "1",
        "3",
        "4",
        "5",
        "2.0",
        "1.0",
        "1.0"
      ) ++ Seq
        .fill(24)("0.0")

      data.slice(1, 9) ===
        Seq(
          Seq(
            "[[User:FromCrimeaNew|FromCrimeaNew]]",
            "1.0",
            "0",
            "0",
            "1",
            "3.0",
            "1",
            "3.0",
            "0.0",
            "0.0"
          ) ++ Seq.fill(24)("0.0"),
          Seq(
            "[[User:FromPodillyaNew1|FromPodillyaNew1]]",
            "1.0",
            "0",
            "0",
            "1",
            "3.0",
            "1",
            "0.0",
            "3.0",
            "0.0"
          ) ++ Seq.fill(24)("0.0"),
          Seq(
            "[[User:FromPodillyaNew2|FromPodillyaNew2]]",
            "1.0",
            "0",
            "0",
            "1",
            "3.0",
            "1",
            "0.0",
            "3.0",
            "0.0"
          ) ++ Seq.fill(24)("0.0"),
          Seq(
            "[[User:FromVolynNew|FromVolynNew]]",
            "1.0",
            "0",
            "0",
            "1",
            "3.0",
            "1",
            "0.0",
            "0.0",
            "3.0"
          ) ++ Seq.fill(24)("0.0"),
          Seq(
            "[[User:FromCrimeaOld|FromCrimeaOld]]",
            "1.0",
            "0",
            "1",
            "0",
            "1.0",
            "1",
            "1.0",
            "0.0",
            "0.0"
          ) ++ Seq.fill(24)("0.0")
        )
    }
  }
}
