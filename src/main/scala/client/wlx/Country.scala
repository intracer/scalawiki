package client.wlx

class Country(
               val code: String,
               val name: String,
               val languageCode: String) {

}

object Country {
  val Ukraine = new Country("ua", "Ukraine", "uk")
}
