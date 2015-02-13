package client.board

class Resolution(
                  val number: String,
                  val date: String,
                  val description: String,
                  val text: String,
                  val votes: Votes,
                  val board: Board,
                  val protocol: Option[BoardProtocol] = None)
