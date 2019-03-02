package com.whosin

import com.whosin.db.slickext.ExtendedPostgresProfile

/**
  * @author tonyl
  */

package object db {
  val profile = new ExtendedPostgresProfile()

  import profile.api._

  val database: Database = Database.forConfig("db")

  val rollCalls = TableQuery[RollCalls]
  val rollCallResponses = TableQuery[RollCallResponses]
}
