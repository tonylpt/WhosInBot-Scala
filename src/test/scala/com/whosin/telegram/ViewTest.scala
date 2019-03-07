package com.whosin.telegram

import com.whosin.actors.ChatCommands._
import com.whosin.domain.AttendanceStatus.{In, Maybe, Out}
import com.whosin.domain.{RollCall, RollCallResponse, RollCallStatus}
import com.whosin.telegram.View._
import org.scalatest.{Matchers, WordSpecLike}

/**
  * @author tonyl
  */

class ViewTest extends WordSpecLike with Matchers {
  "getReplyText" should {
    "return correct result on CommandFailure" in {
      val input = CommandFailure(0, new Exception("error"))
      getReplyText(input) should include("error has occurred")
    }

    "return correct result on NoActiveRollCallResponse" in {
      val input = NoActiveRollCallResponse(0)
      getReplyText(input) should include("No roll call in progress")
    }

    "return correct result on StartRollCallResponse" in {
      val input = StartRollCallResponse(0)
      getReplyText(input) should include("Roll call started")
    }

    "return correct result on EndRollCallResponse" in {
      val input = EndRollCallResponse(0)
      getReplyText(input) should include("Roll call ended")
    }

    "return correct result on UpdateTitleResponse" in {
      val input = UpdateTitleResponse(0, fixture.call)
      getReplyText(input) should include("Roll call title set.")
    }

    "return correct result on UpdateQuietResponse with quiet = false" in {
      val input = UpdateQuietResponse(0, fixture.call, fixture.responses)
      val result = getReplyText(input)

      result should include("In (1)")
      result should include("Maybe (1)")
      result should include("Out (1)")
      result should include("User 1 (will come)")
      result should include("User 2 (might come)")
      result should include("User 3 (won't come)")
    }

    "return correct result on UpdateQuietResponse with quiet = true" in {
      val input = UpdateQuietResponse(0, fixture.call.copy(quiet = true), fixture.responses)
      val result = getReplyText(input)

      result should not include "User 1 (will come)"
      result should not include "User 2 (might come)"
      result should not include "User 3 (won't come)"
    }

    "return correct result on UpdateAttendanceSelfResponse" in {
      val input = UpdateAttendanceSelfResponse(0, "User 1", In, fixture.call, fixture.responses)
      val result = getReplyText(input)

      result should include("User 1 is in")
      result should include("In (1)")
      result should include("Maybe (1)")
      result should include("Out (1)")
      result should include("User 1 (will come)")
      result should include("User 2 (might come)")
      result should include("User 3 (won't come)")
    }

    "return correct result on UpdateAttendanceForResponse" in {
      val input = UpdateAttendanceForResponse(0, "User 2", Maybe, fixture.call, fixture.responses)
      val result = getReplyText(input)

      result should include("User 2 might come")
      result should include("In (1)")
      result should include("Maybe (1)")
      result should include("Out (1)")
      result should include("User 1 (will come)")
      result should include("User 2 (might come)")
      result should include("User 3 (won't come)")
    }

    "return correct result on GetAllAttendanceResponse when quiet = false" in {
      val input = GetAllAttendanceResponse(0, fixture.call, fixture.responses)
      val result = getReplyText(input)

      result should include("call title")
      result should include("In (1)")
      result should include("Maybe (1)")
      result should include("Out (1)")
      result should include("User 1 (will come)")
      result should include("User 2 (might come)")
      result should include("User 3 (won't come)")
    }

    "return correct result on GetAllAttendanceResponse when quiet = true" in {
      val input = GetAllAttendanceResponse(0, fixture.call.copy(quiet = true), fixture.responses)
      val result = getReplyText(input)

      result should not include "User 1 (will come)"
      result should not include "User 2 (might come)"
      result should not include "User 3 (won't come)"
      result should include("Total: 1 in, 1 out, 1 might come.")
    }

  }

  object fixture {
    val call = RollCall(Some(1), 0, RollCallStatus.Open, "call title", quiet = false)
    val responses = List(
      RollCallResponse.createSelfResponse(1, "User 1", In, "will come", 1),
      RollCallResponse.createForResponse("User 2", Maybe, "might come", 1),
      RollCallResponse.createForResponse("User 3", Out, "won't come", 1),
    )
  }

}
