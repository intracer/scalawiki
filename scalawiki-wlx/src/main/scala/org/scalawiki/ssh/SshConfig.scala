package org.scalawiki.ssh

class SshConfig(val user: String,
                val host: String = "login.tools.wmflabs.org",
                val port: Int = 22,
                val privateKeyPath: String = "~/.ssh/id_rsa",
                val passPhrase: String = "",
                val knownHostsPath: String = "~/.ssh/known_hosts",
                val serverHostKeyType: String = "ecdsa-sha2-nistp256")
