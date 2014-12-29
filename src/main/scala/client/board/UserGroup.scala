package client.board

import client.dto.User
import org.joda.time.LocalDate

class UserGroup(val name: String, val users: Set[User]) {

}

class Board(val year: Int, users: Set[User]) extends UserGroup("Правління", users)


