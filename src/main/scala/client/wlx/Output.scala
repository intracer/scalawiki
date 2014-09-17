package client.wlx

import client.wlx.dto.Contest

class Output {

  def monumentsPictured(imageDb: ImageDB, monumentsDb:  MonumentDB) = {

    val contests = (2012 to 2014).map(year => Contest.WLMUkraine(year.toString, "01-09", "31-09"))


  }


}
