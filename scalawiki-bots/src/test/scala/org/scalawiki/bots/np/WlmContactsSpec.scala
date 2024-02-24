package org.scalawiki.bots.np

import org.specs2.mutable.Specification

class WlmContactsSpec extends Specification {

  "WlmContacts" should {
    val numbers = WlmContacts.getNumbers
    "pick 10 digits" in {
      numbers.contains("380931234567") === true
    }

    "pick 12 digits" in {
      numbers.contains("380671234567") === true
    }

    "pick dashed/spaced/braced digits" in {
      numbers.contains("380631234567") === true
      numbers.contains("380971234567") === true
      numbers.contains("380951234567") === true
      numbers.contains("380951234567") === true
      numbers.contains("380971234567") === true
      numbers.contains("380631234567") === true
      numbers.contains("380501234567") === true
    }
  }
}
