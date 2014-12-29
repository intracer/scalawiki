package client.board

import org.joda.time.LocalDate

class Resolution(
                  val number: String,
                  val date: LocalDate,
                  val description: String,
                  val text: String,
                  val votes: Votes,
                  val board: Board,
                  val meeting: Option[BoardMeeting] = None)
