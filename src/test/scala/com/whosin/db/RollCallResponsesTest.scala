package com.whosin.db

import com.whosin.db.Util.{cleanDB, _}
import com.whosin.db.profile.api._
import com.whosin.domain.AttendanceStatus.{In, Maybe, Out}
import com.whosin.domain.{RollCall, RollCallResponse}
import org.scalatest.{BeforeAndAfter, Matchers, WordSpec}

/**
  * @author tonyl
  */
class RollCallResponsesTest extends WordSpec with Matchers with BeforeAndAfter {

  var call: RollCall = _

  before {
    cleanDB()
    call = RollCalls.insertAndCleanUp(0, "a new call").await()
  }

  "addOrUpdate" should {
    "add a new response" in {
      val response = RollCallResponse.createSelfResponse(1, "User 1", Maybe, "might come", call.id.get)
      val result = RollCallResponses.addOrUpdate(response).await()
      result.id.isDefined shouldBe true
      result.userId shouldEqual response.userId
      result.username shouldEqual response.username
      result.status shouldEqual response.status
      result.reason shouldEqual response.reason

      val queried: RollCallResponse = awaitDB(rollCallResponses.result).head
      queried shouldEqual result
    }

    "update an existing response" in {
      val response = RollCallResponse.createSelfResponse(1, "User 1", Maybe, "might come", call.id.get)
      RollCallResponses.addOrUpdate(response).await()

      val before = awaitDB(rollCallResponses.result).single
      before.userId shouldEqual response.userId
      before.username shouldEqual response.username
      before.status shouldEqual response.status
      before.reason shouldEqual response.reason
      before.updatedAt shouldEqual before.createdAt

      val updatedResponse = response.copy(username = "user 1", status = In, reason = "will come")
      RollCallResponses.addOrUpdate(updatedResponse).await()

      val after = awaitDB(rollCallResponses.result).single
      after.id shouldEqual before.id
      after.userId shouldEqual updatedResponse.userId
      after.username shouldEqual updatedResponse.username
      after.status shouldEqual updatedResponse.status
      after.reason shouldEqual updatedResponse.reason
      after.updatedAt.after(after.createdAt) shouldBe true
    }
  }

  "getAll" should {
    "return all existing responses" in {
      val responses = List(
        RollCallResponse.createSelfResponse(1, "User 1", Maybe, "might come", call.id.get),
        RollCallResponse.createForResponse("User 2", Out, "won't come", call.id.get)
      )

      val inserted = responses.map(RollCallResponses.addOrUpdate(_).await())
      val result = RollCallResponses.getAll(call.id.get).await()
      result shouldEqual inserted
    }
  }

}
