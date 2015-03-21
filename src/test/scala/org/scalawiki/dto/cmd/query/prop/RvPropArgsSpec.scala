package org.scalawiki.dto.cmd.query.prop

import org.scalawiki.dto.cmd.query.prop.rvprop.Ids
import org.specs2.mutable.Specification
import org.scalawiki.dto.cmd.query.prop.rvprop._

class RvPropArgsSpec extends Specification {
  "RvPropArgs" should {
    "get RvPropArgs by name" in {
      RvPropArgs.byNames(Seq("ids")) === Seq(Ids)
      RvPropArgs.byNames(Seq("user", "userid")) === Seq(User, UserId)
    }
  }

}
