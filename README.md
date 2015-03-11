# scalawiki
scalawiki is a MediaWiki client in Scala

[![Build Status](https://travis-ci.org/intracer/scalawiki.svg?branch=master)](https://travis-ci.org/intracer/scalawiki?branch=master)
[![Coverage Status](https://coveralls.io/repos/intracer/scalawiki/badge.svg)](https://coveralls.io/r/intracer/scalawiki)
[![Codacy Badge](https://www.codacy.com/project/badge/83a1a032be754d0c81b87e9633988ae2)](https://www.codacy.com/public/intracer/scalawiki)
[![Join the chat at https://gitter.im/intracer/scalawiki](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/intracer/scalawiki?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

On early stages of development but already has features many clients lack.

Why [another client library for MediaWiki](https://www.mediawiki.org/wiki/API:Client_code)?

Well many of them are very basic, not to say primitive. For example JWBF [only recently] (https://github.com/eldur/jwbf/issues/21) got the ability to query more than 1 page at a time. I don't know any Java client that supports [generators](https://www.mediawiki.org/wiki/API:Query#Generators) (fetching properties from articles listed by list query in a single request).

When Wikipedia sites are real examples of Big Data it is just a show stopper. Fetching information about Wiki Loves Monuments uploads in such ineffective way will take almost a day even for one country, when could be done in several minutes otherwise in batches of 5000 (recently Wikimedia decreased max limit to 500 and that really slowed thing down a bit, but anyway).

# Roadmap
1. First goal is to 
  * Fully support [MediaWiki API](https://www.mediawiki.org/wiki/API:Main_page), maybe without  [WikiData](https://meta.wikimedia.org/wiki/Wikidata) support at first. This means all the possible API parameters. Don't know if any other API client library supports MediaWiki API fully, maybe  [pywikibot](https://www.mediawiki.org/wiki/Manual:Pywikibot) does. Most others support only some very limited subset.
  * Support different backends - MediaWiki API, [xml dumps](https://meta.wikimedia.org/wiki/Data_dumps), [MediWiki database](https://www.mediawiki.org/wiki/Manual:Database_layout). Support coping data between backends (importing and exporting xml dumps to database, storing data retrived by MediaWiki API to xml dumps or database).
  * Excercise the library with different applications
2. Next will come 
  * client library API simplifications. My first attempt to write generic code quickly became complex handling of special cases, so for now I will concentrate on full API support, and after having the full picture will try to come with ideas how to structure the client library better
  * speed optimizations (for xml dumps: indexing, parallel indexed bzip2, fast lz4 compression, according to benchmarks ([compression](http://jpountz.github.io/lz4-java/1.2.0/lz4-compression-benchmark/), [decompression](http://jpountz.github.io/lz4-java/1.2.0/lz4-decompression-benchmark/)) lz4 is about 6 times faster than gzip and 50 time faster than bzip2, try [EXI XML](http://exificient.sourceforge.net/), direct processing of utf-8 data (by default java uses utf-16, according to available estimations about half of the cpu load in xml parsing is related to encoding conversion, this percentage can be even higher for simpler json and database access), etc
