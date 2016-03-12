package org.scalawiki.dto.history

import com.github.nscala_time.time.Imports._
import org.scalawiki.dto.Revision
import org.scalawiki.dto.filter.RevisionFilterDateAndUser
import org.specs2.mutable.Specification

class RevisionFilterSpec extends Specification {

  "revision filter by date"  should {

    val now: DateTime = DateTime.now

    "filter from" in {
      val r1 = Revision(1, 1).withTimeStamp(now - 2.months)
      val r2 = Revision(2, 1).withTimeStamp(now)

      val rf = new RevisionFilterDateAndUser(from = Some(now - 1.month))

      val filtered = rf(Seq(r1,r2))

      filtered.size === 1
      filtered.head.revId === Some(2)
    }

    "filter to" in {
      val r1 = Revision(1, 1).withTimeStamp(now - 2.months)
      val r2 = Revision(2, 1).withTimeStamp(now)

      val rf = new RevisionFilterDateAndUser(to = Some(now - 1.month))

      val filtered = rf(Seq(r1,r2))

      filtered.size === 1
      filtered.head.revId === Some(1)
    }

    "filter from and to" in {
      val r1 = Revision(1, 1).withTimeStamp(now - 3.months)
      val r2 = Revision(2, 1).withTimeStamp(now - 2.months)
      val r3 = Revision(3, 1).withTimeStamp(now - 1.months)
      val r4 = Revision(4, 1).withTimeStamp(now)

      val rf = new RevisionFilterDateAndUser(from = Some(now - 2.month), to = Some(now - 1.month))

      val filtered = rf(Seq(r1,r2, r3, r4))

      filtered.size === 2
      filtered.map(_.revId) === Seq(2, 3).map(Option.apply)
    }
  }

}
