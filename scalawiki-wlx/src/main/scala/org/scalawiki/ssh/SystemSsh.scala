package org.scalawiki.ssh


trait SystemSsh {
  this: SshConfig =>

  def connect() = {

    import sys.process._

    val cmd = s"ssh $user@$host"

    cmd !

  }


}

