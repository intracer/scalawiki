#!/bin/bash

function package_absent() {
    return dpkg -l "$1" &> /dev/null
}

if package_absent oracle-java8-installer ; then
    echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee /etc/apt/sources.list.d/webupd8team-java.list
    echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee -a /etc/apt/sources.list.d/webupd8team-java.list

    # Accept license non-interactive
    echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886
    apt-get update
    apt-get install -y oracle-java8-installer

    # Make sure Java 8 becomes default java
    apt-get install -y oracle-java8-set-default
fi

if package_absent scala ; then
    wget http://www.scala-lang.org/files/archive/scala-2.11.8.deb
    dpkg -i scala-2.11.8.deb
    apt-get -y update
    apt-get -y install scala
fi

if package_absent sbt ; then
    wget http://dl.bintray.com/sbt/debian/sbt-0.13.11.deb
    dpkg -i sbt-0.13.11.deb
    apt-get -y update
    apt-get -y install sbt
fi