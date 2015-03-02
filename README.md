# scalawiki
scalawiki is a MediaWiki client in Scala

[![Build Status](https://travis-ci.org/intracer/scalawiki.svg?branch=master)](https://travis-ci.org/intracer/scalawiki?branch=master)
[![Coverage Status](https://coveralls.io/repos/intracer/scalawiki/badge.svg)](https://coveralls.io/r/intracer/scalawiki)
[![Join the chat at https://gitter.im/intracer/scalawiki](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/intracer/scalawiki?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

On early stages of development but already has features many clients lack.

Why [another client library for MediaWiki](https://www.mediawiki.org/wiki/API:Client_code)?

Well many of them are very basic, not to say primitive. For example JWBF [only recently] (https://github.com/eldur/jwbf/issues/21) got the ability to query more than 1 page at a time. I don't know any Java client that supports generators (fetching properties from articles listed by list query in a single request).

When Wikipedia sites are real examples of Big Data it is just a show stopper. Fethcing information about Wiki Loves Monuments uploads in such ineffective way will take almost a day even for one country, when could be done in several minutes otherwise in batches of 5000 (recently Wikimedia decreased max limit to 500 and that really slowed thing down a bit, but anyway).

