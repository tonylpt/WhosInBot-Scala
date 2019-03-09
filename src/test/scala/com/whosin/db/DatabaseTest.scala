package com.whosin.db

import com.whosin.db.Util._
import com.whosin.db.profile.api._
import slick.dbio.{DBIOAction, NoStream}

/**
  * @author tony
  */

trait DatabaseTest {

  private val _ = MigrationOnce

  def awaitDB[R](a: DBIOAction[R, NoStream, Nothing]): R = {
    database.run(a).await()
  }

  def cleanDB(): Unit = {
    val seq = List(
      sqlu"TRUNCATE w_roll_calls, w_roll_call_responses CASCADE",
    )

    awaitDB(DBIO.sequence(seq))
  }

}

object MigrationOnce {
  Migration.migrate()
}
