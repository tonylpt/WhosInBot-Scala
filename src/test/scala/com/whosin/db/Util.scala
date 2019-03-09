package com.whosin.db

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * @author tonyl
  */

object Util {

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
