package client.dto

class User(val login: String, val name: String) extends Ordered[User] {

  override def compare(that: User): Int = login.compareToIgnoreCase(that.login)

  override def hashCode(): Int = login.hashCode()

  override def equals(obj: scala.Any): Boolean = login.equals(obj)

  override def toString: String = login
}
