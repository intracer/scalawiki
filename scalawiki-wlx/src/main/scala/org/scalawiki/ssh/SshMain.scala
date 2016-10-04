package org.scalawiki.ssh

object SshMain {

  def main(args: Array[String]) {

    val ssh = new SshConfig(user = "ilya") with JavaSsh

    val session = ssh.connect()
  }
}
