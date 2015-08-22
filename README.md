# scalawiki
scalawiki is an experimental MediaWiki client in Scala on early stages of development.

[![Build Status](https://travis-ci.org/intracer/scalawiki.svg?branch=master)](https://travis-ci.org/intracer/scalawiki?branch=master)
[![Coverage Status](https://coveralls.io/repos/intracer/scalawiki/badge.svg)](https://coveralls.io/r/intracer/scalawiki)
[![Codacy Badge](https://www.codacy.com/project/badge/83a1a032be754d0c81b87e9633988ae2)](https://www.codacy.com/public/intracer/scalawiki)
[![Join the chat at https://gitter.im/intracer/scalawiki](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/intracer/scalawiki?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[ ![Download](https://api.bintray.com/packages/intracer/maven/scalawiki/images/download.svg) ](https://bintray.com/intracer/maven/scalawiki/_latestVersion)


Why [another client library for MediaWiki](https://www.mediawiki.org/wiki/API:Client_code)?

I don't know any Java client that supports [generators](https://www.mediawiki.org/wiki/API:Query#Generators) (fetching properties from articles listed by list query in a single request). JWBF [only recently] (https://github.com/eldur/jwbf/issues/21) got the ability to query more than 1 page at a time. 

When Wikipedia sites are real examples of Big Data it is just a show stopper. Fetching information about Wiki Loves Monuments uploads in such ineffective way will take almost a day even for one country, when could be done in several minutes otherwise in batches of 5000 (recently Wikimedia decreased max limit to 500 and that really slowed thing down a bit, but anyway).

This library uses [Scala Futures](http://docs.scala-lang.org/overviews/core/futures.html) for easy job parallelization, later may use [Akka actors](http://akka.io/docs/) and Akka Streams 


# Goals
1. First goal is to 
  * Fully support [MediaWiki API](https://www.mediawiki.org/wiki/API:Main_page)
  * Support different backends - MediaWiki API, [xml dumps](https://meta.wikimedia.org/wiki/Data_dumps), [MediWiki database](https://www.mediawiki.org/wiki/Manual:Database_layout). Support coping data between backends (importing and exporting xml dumps to database, storing data retrived by MediaWiki API to xml dumps or database).
  * Good test coverage
