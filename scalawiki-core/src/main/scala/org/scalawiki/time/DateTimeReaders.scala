package org.scalawiki.time


import com.typesafe.config.Config
import com.typesafe.config.ConfigException._
import com.typesafe.config.ConfigValueType._

import net.ceedubs.ficus.readers.{DurationReaders, ValueReader}

import org.joda.time.DateTime

/**
  * From https://github.com/solar/ficus-readers/blob/master/jodatime/src/main/scala/org/sazabi/ficus/readers/jodatime/DateTimeReader.scala
  */
trait DateTimeReaders {
  implicit val DateTimeReader: ValueReader[DateTime] = new ValueReader[DateTime] {
    def read(config: Config, path: String): DateTime = config.getValue(path).valueType match {
      case NUMBER => new DateTime(config.getLong(path))
      case STRING => try (new DateTime(config.getString(path))) catch {
        case e: IllegalArgumentException => throw new BadValue(path, e.getMessage, e)
      }
      case t => throw new WrongType(config.origin, path,
        "NUMBER(milliseconds) or STRING(ISO-8601)", t.toString)
    }
  }
}

trait Readers extends DurationReaders with DateTimeReaders

object imports extends Readers