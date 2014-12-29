package client.board

import client.dto.User

class Votes(
             val support: Set[User],
             val oppose: Set[User],
             val abstain: Set[User]) {



}
