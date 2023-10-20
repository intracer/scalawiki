package org.scalawiki.bots.np

import org.specs2.mutable.Specification

class WlmContactsSpec extends Specification{

    "WlmContacts" should {
      "pick 10 digits" in {
        WlmContacts.getNumbers.contains("0932563441") === true
      }

      "pick 12 digits" in {
        WlmContacts.getNumbers.contains("380675488100") === true
      }

      "pick dashed/spaced/braced digits" in {
        WlmContacts.getNumbers.contains("063-370-49-59") === true
        WlmContacts.getNumbers.contains("097 535-70-10") === true
        WlmContacts.getNumbers.contains("095 664 17 25") === true
        WlmContacts.getNumbers.contains("095-0415177") === true
        WlmContacts.getNumbers.contains("(097)9398937") === true
        WlmContacts.getNumbers.contains("380 (63) 788 27 00") === true
        WlmContacts.getNumbers.contains("38 050 801 52 49") === true


      }
    }
}
