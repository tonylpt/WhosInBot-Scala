package com.whosin.db

import java.sql.Timestamp
import java.util.Date

/**
  * @author tonyl
  */
object Implicits {

  implicit class TimestampExt(val t: Timestamp) extends AnyVal {
    def toDate: Date = new Date(t.getTime)
  }

  implicit class DateExt(val d: Date) extends AnyVal {
    def toTimestamp: Timestamp = new Timestamp(d.getTime)
  }

}
