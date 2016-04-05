apt-get update
apt-get install -y openjdk-7-jdk

wget http://www.scala-lang.org/files/archive/scala-2.11.8.deb
dpkg -i scala-2.11.8.deb
apt-get -y update
apt-get -y install scala

wget http://dl.bintray.com/sbt/debian/sbt-0.13.11.deb
dpkg -i sbt-0.13.11.deb
apt-get -y update
apt-get -y install sbt

