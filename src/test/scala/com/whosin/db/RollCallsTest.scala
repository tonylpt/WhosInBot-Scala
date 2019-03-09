package com.whosin.db

import com.whosin.db.Util._
import com.whosin.db.profile.api._
import com.whosin.domain.RollCallStatus.Open
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * @author tonyl
  */
class RollCallsTest extends WordSpec with Matchers with BeforeAndAfter {

  before {
    cleanDB()
  }

  "insertAndCleanUp" should {
    "insert and return a new open call with an id" in {
      val call = RollCalls.insertAndCleanUp(0, "a new call").await()
      call.id.isDefined shouldEqual true
      call.title shouldEqual "a new call"
      call.status shouldEqual Open
      call.quiet shouldBe false

      val calls = awaitDB(rollCalls.result)
      calls.size shouldEqual 1
      calls.head.title shouldEqual call.title
    }

    "close all existing calls" in {
      RollCalls.insertAndCleanUp(1, "a new call 1").await()
      val calls = awaitDB(rollCalls.filter(_.status === Open.entryName).result)
      calls.size shouldEqual 1
      calls.head.title shouldEqual "a new call 1"

      RollCalls.insertAndCleanUp(1, "a new call 2").await()
      val calls2 = awaitDB(rollCalls.filter(_.status === Open.entryName).result)
      calls2.size shouldEqual 1
      calls2.head.title shouldEqual "a new call 2"
    }

    "clear all old calls" in {
      Future.sequence((1 to 20).map { i =>
        RollCalls.insertAndCleanUp(0, s"a new call $i")
      }).await()

      val calls = awaitDB(rollCalls.result)
      calls.size shouldEqual (RollCalls.CleanupThreshold + 1)
      calls.map(_.title) shouldEqual (10 to 20).map(i => s"a new call $i")
    }
  }

  "closeCurrent" should {
    "close the current call" in {
      RollCalls.insertAndCleanUp(0, "call 1").await()
      awaitDB(rollCalls.filter(_.status === Open.entryName).result).size shouldEqual 1

      RollCalls.closeCurrent(0)
      awaitDB(rollCalls.filter(_.status === Open.entryName).result).size shouldEqual 0
    }
  }

  "getCurrent" should {
    "return the current open call" in {
      RollCalls.insertAndCleanUp(0, "call 1").await()
      val current = RollCalls.getCurrent(0).await().get

      current.title shouldEqual "call 1"
      current.quiet shouldBe false
      current.status shouldEqual Open
    }
  }

  "setCurrentTitle" should {
    "return None when there is no open roll call" in {
      val result = RollCalls.setCurrentTitle(0, "new title").await()
      result shouldBe None
    }

    "update title and returns updated call" in {
      RollCalls.insertAndCleanUp(0, "call 1").await()
      val currentCall = RollCalls.getCurrent(0).await().get

      currentCall.title shouldEqual "call 1"
      currentCall.status shouldEqual Open
      currentCall.quiet shouldBe false

      val result = RollCalls.setCurrentTitle(0, "new title").await().get
      result shouldEqual RollCalls.getCurrent(0).await().get
      result shouldEqual currentCall.copy(title = "new title")
    }
  }

  "setCurrentQuiet" should {
    "return None when there is no open roll call" in {
      val result = RollCalls.setCurrentQuiet(0, quiet = true).await()
      result shouldBe None
    }

    "update quietness and returns updated call" in {
      RollCalls.insertAndCleanUp(0, "call 1").await()
      val currentCall = RollCalls.getCurrent(0).await().get

      currentCall.title shouldEqual "call 1"
      currentCall.status shouldEqual Open
      currentCall.quiet shouldBe false

      val result = RollCalls.setCurrentQuiet(0, quiet = true).await().get
      result shouldEqual RollCalls.getCurrent(0).await().get
      result shouldEqual currentCall.copy(quiet = true)
    }
  }
}
