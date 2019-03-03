package com.whosin.actors

import akka.actor.ActorRef
import akka.testkit.TestProbe
import com.whosin.actors.ChatCommands.{UpdateAttendanceSelfResponse, _}
import com.whosin.db.{RollCallRepo, RollCallResponseRepo}
import com.whosin.domain.AttendanceStatus.{In, Maybe, Out}
import com.whosin.domain.RollCallResponse.{createForResponse, createSelfResponse}
import com.whosin.domain.RollCallStatus.Open
import com.whosin.domain._
import org.scalamock.scalatest.MockFactory

import scala.concurrent.Future

/**
  * @author tonyl
  */

class ChatActorTest extends BaseActorTest("ChatActorTest") with MockFactory {

  val chatId: ChatId = 1234L
  val callId: RollCallId = 1L

  val rollCall = RollCall(Some(callId), chatId, Open, "call title", quiet = false)
  val response1: RollCallResponse = createSelfResponse(1, "User 1", In, "will come", callId)
  val response2: RollCallResponse = createForResponse("User 1", Out, "won't come", callId)
  val rollCallResponses = List(response1, response2)

  val databaseError = new Exception("mock error, please ignore")

  var rollCallRepo: RollCallRepo = _
  var rollCallResponseRepo: RollCallResponseRepo = _
  var chatActor: ActorRef = _
  var probe: TestProbe = _

  before {
    rollCallRepo = stub[RollCallRepo]
    rollCallResponseRepo = stub[RollCallResponseRepo]
    chatActor = system.actorOf(ChatActor.props(chatId, rollCallRepo, rollCallResponseRepo))
    probe = TestProbe()
  }

  "when receive StartRollCall" should {
    "reply with StartRollCallResponse if successful" in {
      (rollCallRepo.insertAndCleanUp _)
        .when(chatId, "call title")
        .returns(Future.successful(rollCall))

      chatActor.tell(StartRollCall(chatId, "call title"), probe.ref)
      probe.expectMsg(StartRollCallResponse(chatId))
    }

    "reply with CommandFailure if failed" in {
      (rollCallRepo.insertAndCleanUp _)
        .when(*, *)
        .returns(Future.failed(databaseError))

      chatActor.tell(StartRollCall(chatId, "call title"), probe.ref)
      probe.expectMsg(CommandFailure(chatId, databaseError))
    }
  }

  "when receive EndRollCall" should {
    "reply with EndRollCallResponse if successful" in {
      (rollCallRepo.closeCurrent _)
        .when(chatId)
        .returns(Future.successful(true))

      chatActor.tell(EndRollCall(chatId), probe.ref)
      probe.expectMsg(EndRollCallResponse(chatId))
    }

    "reply with NoActiveRollCallResponse if call not found" in {
      (rollCallRepo.closeCurrent _)
        .when(chatId)
        .returns(Future.successful(false))

      chatActor.tell(EndRollCall(chatId), probe.ref)
      probe.expectMsg(NoActiveRollCallResponse(chatId))
    }

    "reply with CommandFailure if failed" in {
      (rollCallRepo.closeCurrent _)
        .when(*)
        .returns(Future.failed(databaseError))

      chatActor.tell(EndRollCall(chatId), probe.ref)
      probe.expectMsg(CommandFailure(chatId, databaseError))
    }
  }

  "when receive UpdateTitle" should {
    val newTitle = "new title"

    "reply with UpdateTitleResponse if successful" in {
      val newCall = rollCall.copy(title = newTitle)

      (rollCallRepo.setCurrentTitle _)
        .when(chatId, newTitle)
        .returns(Future.successful(Some(newCall)))

      chatActor.tell(UpdateTitle(chatId, newTitle), probe.ref)
      probe.expectMsg(UpdateTitleResponse(chatId, newCall))
    }

    "reply with NoActiveRollCallResponse if call not found" in {
      (rollCallRepo.setCurrentTitle _)
        .when(chatId, newTitle)
        .returns(Future.successful(None))

      chatActor.tell(UpdateTitle(chatId, newTitle), probe.ref)
      probe.expectMsg(NoActiveRollCallResponse(chatId))
    }

    "reply with CommandFailure if failed" in {
      (rollCallRepo.setCurrentTitle _)
        .when(*, *)
        .returns(Future.failed(databaseError))

      chatActor.tell(UpdateTitle(chatId, newTitle), probe.ref)
      probe.expectMsg(CommandFailure(chatId, databaseError))
    }
  }

  "when receive UpdateQuiet" should {
    val newQuiet = true

    "reply with UpdateQuietResponse if successful" in {
      val newCall = rollCall.copy(quiet = newQuiet)

      (rollCallRepo.setCurrentQuiet _)
        .when(chatId, newQuiet)
        .returns(Future.successful(Some(newCall)))

      (rollCallResponseRepo.getAll _)
        .when(callId)
        .returns(Future.successful(rollCallResponses))

      chatActor.tell(UpdateQuiet(chatId, newQuiet), probe.ref)
      probe.expectMsg(UpdateQuietResponse(chatId, newCall, rollCallResponses))
    }

    "reply with NoActiveRollCallResponse if call not found" in {
      (rollCallRepo.setCurrentQuiet _)
        .when(chatId, newQuiet)
        .returns(Future.successful(None))

      chatActor.tell(UpdateQuiet(chatId, newQuiet), probe.ref)
      probe.expectMsg(NoActiveRollCallResponse(chatId))
    }

    "reply with CommandFailure if roll call DB failed" in {
      (rollCallRepo.setCurrentQuiet _)
        .when(*, *)
        .returns(Future.failed(databaseError))

      chatActor.tell(UpdateQuiet(chatId, newQuiet), probe.ref)
      probe.expectMsg(CommandFailure(chatId, databaseError))
    }

    "reply with CommandFailure if roll call response DB failed" in {
      val newCall = rollCall.copy(quiet = newQuiet)

      (rollCallRepo.setCurrentQuiet _)
        .when(chatId, newQuiet)
        .returns(Future.successful(Some(newCall)))

      (rollCallResponseRepo.getAll _)
        .when(callId)
        .returns(Future.failed(databaseError))

      chatActor.tell(UpdateQuiet(chatId, newQuiet), probe.ref)
      probe.expectMsg(CommandFailure(chatId, databaseError))
    }
  }

  "when receive UpdateAttendanceSelf" should {
    val newResponse = response1.copy(status = Out, reason = "changed my mind")
    val newResponses = rollCallResponses.map {
      case `response1` => newResponse
      case other => other
    }

    val command = UpdateAttendanceSelf(chatId, newResponse.userId, newResponse.username, newResponse.status, newResponse.reason)

    "reply with UpdateQuietResponse if successful" in {
      (rollCallRepo.getCurrent _)
        .when(chatId)
        .returns(Future.successful(Some(rollCall)))

      (rollCallResponseRepo.addOrUpdate _)
        .when(where[RollCallResponse] { res =>
          res.status == newResponse.status && res.reason == newResponse.reason
        })
        .returns(Future.successful(newResponse))

      (rollCallResponseRepo.getAll _)
        .when(callId)
        .returns(Future.successful(newResponses))

      chatActor.tell(command, probe.ref)

      probe.expectMsg(
        UpdateAttendanceSelfResponse(chatId, newResponse.username, newResponse.status, rollCall, newResponses)
      )
    }

    "reply with NoActiveRollCallResponse if call not found" in {
      (rollCallRepo.getCurrent _)
        .when(chatId)
        .returns(Future.successful(None))

      chatActor.tell(command, probe.ref)
      probe.expectMsg(NoActiveRollCallResponse(chatId))
    }

    "reply with CommandFailure if roll call DB failed" in {
      (rollCallRepo.getCurrent _)
        .when(*)
        .returns(Future.failed(databaseError))

      chatActor.tell(command, probe.ref)
      probe.expectMsg(CommandFailure(chatId, databaseError))
    }

    "reply with CommandFailure if roll call response DB failed" in {
      (rollCallRepo.getCurrent _)
        .when(chatId)
        .returns(Future.successful(Some(rollCall)))

      (rollCallResponseRepo.addOrUpdate _)
        .when(*)
        .returns(Future.failed(databaseError))

      chatActor.tell(command, probe.ref)
      probe.expectMsg(CommandFailure(chatId, databaseError))
    }
  }

  "when receive UpdateAttendanceFor" should {
    val newResponse = response2.copy(status = Maybe, reason = "changed my mind")
    val newResponses = rollCallResponses.map {
      case `response2` => newResponse
      case other => other
    }

    val command = UpdateAttendanceFor(chatId, newResponse.username, newResponse.status, newResponse.reason)

    "reply with UpdateQuietResponse if successful" in {
      (rollCallRepo.getCurrent _)
        .when(chatId)
        .returns(Future.successful(Some(rollCall)))

      (rollCallResponseRepo.addOrUpdate _)
        .when(where[RollCallResponse] { res =>
          res.status == newResponse.status && res.reason == newResponse.reason
        })
        .returns(Future.successful(newResponse))

      (rollCallResponseRepo.getAll _)
        .when(callId)
        .returns(Future.successful(newResponses))

      chatActor.tell(command, probe.ref)

      probe.expectMsg(
        UpdateAttendanceForResponse(chatId, newResponse.username, newResponse.status, rollCall, newResponses)
      )
    }

    "reply with NoActiveRollCallResponse if call not found" in {
      (rollCallRepo.getCurrent _)
        .when(chatId)
        .returns(Future.successful(None))

      chatActor.tell(command, probe.ref)
      probe.expectMsg(NoActiveRollCallResponse(chatId))
    }

    "reply with CommandFailure if roll call DB failed" in {
      (rollCallRepo.getCurrent _)
        .when(*)
        .returns(Future.failed(databaseError))

      chatActor.tell(command, probe.ref)
      probe.expectMsg(CommandFailure(chatId, databaseError))
    }

    "reply with CommandFailure if roll call response DB failed" in {
      (rollCallRepo.getCurrent _)
        .when(chatId)
        .returns(Future.successful(Some(rollCall)))

      (rollCallResponseRepo.addOrUpdate _)
        .when(*)
        .returns(Future.failed(databaseError))

      chatActor.tell(command, probe.ref)
      probe.expectMsg(CommandFailure(chatId, databaseError))
    }
  }

  "when receive GetAllAttendance" should {
    "reply with GetAllAttendanceResponse if successful" in {
      (rollCallRepo.getCurrent _)
        .when(chatId)
        .returns(Future.successful(Some(rollCall)))

      (rollCallResponseRepo.getAll _)
        .when(callId)
        .returns(Future.successful(rollCallResponses))

      chatActor.tell(GetAllAttendance(chatId), probe.ref)
      probe.expectMsg(GetAllAttendanceResponse(chatId, rollCall, rollCallResponses))
    }

    "reply with NoActiveRollCallResponse if call not found" in {
      (rollCallRepo.getCurrent _)
        .when(chatId)
        .returns(Future.successful(None))

      chatActor.tell(GetAllAttendance(chatId), probe.ref)
      probe.expectMsg(NoActiveRollCallResponse(chatId))
    }

    "reply with CommandFailure if roll call DB failed" in {
      (rollCallRepo.getCurrent _)
        .when(*)
        .returns(Future.failed(databaseError))

      chatActor.tell(GetAllAttendance(chatId), probe.ref)
      probe.expectMsg(CommandFailure(chatId, databaseError))
    }

    "reply with CommandFailure if roll call response DB failed" in {
      (rollCallRepo.getCurrent _)
        .when(chatId)
        .returns(Future.successful(Some(rollCall)))

      (rollCallResponseRepo.getAll _)
        .when(callId)
        .returns(Future.failed(databaseError))

      chatActor.tell(GetAllAttendance(chatId), probe.ref)
      probe.expectMsg(CommandFailure(chatId, databaseError))
    }
  }
}
