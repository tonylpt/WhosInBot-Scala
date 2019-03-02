package com.whosin.db

import com.whosin.db.profile.api._
import slick.dbio.{DBIOAction, NoStream}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * @author tonyl
  */

object Util {

  def awaitDB[R](a: DBIOAction[R, NoStream, Nothing]): R = {
    database.run(a).await()
  }

  def cleanDB(): Unit = {
    val seq = List(
      sqlu"TRUNCATE w_roll_calls, w_roll_call_responses CASCADE",
    )

    awaitDB(DBIO.sequence(seq))
  }

  implicit class FutureExt[T](val f: Future[T]) extends AnyVal {
    def await(): T = Await.result(f, 10.second)
  }

  implicit class SeqExt[T](val s: Seq[T]) extends AnyVal {
    def single: T = s.toList match {
      case head :: Nil => head
      case _ => throw new AssertionError("Seq is expected to have a single element.")
    }
  }

}
