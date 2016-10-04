package org.scalawiki.ssh

import java.net.Socket
import java.util.concurrent.ThreadLocalRandom

import akka.event.Logging
import akka.event.Logging.LogLevel
import com.jcraft.jsch._
import org.scalawiki.MwBot

import scala.util.Try

trait JavaSsh {

  this: SshConfig =>

  val jsch = new JSch()

  var session: Session = _

  def connect(): Session = {
    jsch.setKnownHosts(knownHostsPath)

    session = jsch.getSession(user, host, port)

    session.setConfig("server_host_key", serverHostKeyType)

    jsch.addIdentity(privateKeyPath, passPhrase)

    session.connect()

    session
  }

  // 4711:commonswiki.labsdb:3306
  def forward(tunnelLocalPort: Int, tunnelRemoteHost: String, tunnelRemotePort: Int) = {
    session.setPortForwardingL(tunnelLocalPort, tunnelRemoteHost, tunnelRemotePort)
  }

  def isPortFree(port: Int, host: String = "localhost"): Boolean = {
    Try {
      new Socket(host, port)
    }.map { s => s.close(); true }.getOrElse(false)
  }

  def randomPort() = ThreadLocalRandom.current().nextInt(49152, 65535)

  def logKnownKeys(jsch: JSch): Unit = {
    val hkr = jsch.getHostKeyRepository
    val hks = hkr.getHostKey
    if (hks != null) {
      println("Host keys in " + hkr.getKnownHostsRepositoryID)
      for (hk <- hks)
        println("host: " + hk.getHost + ", type: " + hk.getType + ", fp: " + hk.getFingerPrint(jsch))
    }
  }

  def enableLogging(): Unit = {
    JSch.setLogger(new Logger {

      def mapLogLevel(level: Int): LogLevel = {
        level match {
          case Logger.DEBUG => Logging.DebugLevel
          case Logger.INFO => Logging.InfoLevel
          case Logger.WARN => Logging.WarningLevel
          case Logger.ERROR => Logging.ErrorLevel
          case Logger.FATAL => Logging.ErrorLevel
        }
      }

      override def log(level: Int, message: String): Unit = {
        MwBot.system.log.log(mapLogLevel(level), message)
      }

      override def isEnabled(level: Int): Boolean = true
    })
  }

}