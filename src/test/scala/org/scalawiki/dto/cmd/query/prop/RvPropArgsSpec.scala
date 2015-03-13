package org.scalawiki.dto.cmd.query.prop

import org.specs2.mutable.Specification

class RvPropArgsSpec extends Specification {

  "RvPropArgs" should {
    "get RvPropArgs by name" in {
      RvPropArgs.byNames(Seq("ids")) === Seq(Ids)
      RvPropArgs.byNames(Seq("user", "userid")) === Seq(User, UserId)
    }
  }

}
