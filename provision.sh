echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee /etc/apt/sources.list.d/webupd8team-java.list
echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee -a /etc/apt/sources.list.d/webupd8team-java.list

# Accept license non-iteractive
echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886
apt-get update
apt-get install -y oracle-java8-installer

# Make sure Java 8 becomes default java
apt-get install -y oracle-java8-set-default

wget http://www.scala-lang.org/files/archive/scala-2.11.8.deb
dpkg -i scala-2.11.8.deb
apt-get -y update
apt-get -y install scala

wget http://dl.bintray.com/sbt/debian/sbt-0.13.11.deb
dpkg -i sbt-0.13.11.deb
apt-get -y update
apt-get -y install sbt

# JavaFX dependencies
apt-get -y install libswt-gtk-3-jni libswt-gtk-3-java libxslt1.1
