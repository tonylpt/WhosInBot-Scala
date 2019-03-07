package com.whosin.actors

import akka.actor.{Actor, Props}
import akka.testkit.TestProbe
import com.whosin.actors.ChatCommands.{EndRollCall, EndRollCallResponse, StartRollCall, StartRollCallResponse}
import com.whosin.domain.ChatId

import scala.concurrent.duration._

/**
  * @author tonyl
  */

class ChatThreadManagerTest extends BaseActorTest("ChatThreadManagerTest") {

  var probe: TestProbe = _

  before {
    probe = TestProbe()
  }

  "ChatThreadManager" should {
    "create a child actor to handle each chat ID" in {
      val manager = system.actorOf(ChatThreadManager.props(500.millis, MockChatActor.props))

      manager.tell(StartRollCall(1, "title 1"), probe.ref)
      probe.expectMsg(StartRollCallResponse(1))
      val chatActor1 = probe.lastSender

      manager.tell(StartRollCall(2, "title 2"), probe.ref)
      probe.expectMsg(StartRollCallResponse(2))
      val chatActor2 = probe.lastSender

      chatActor1 should not be chatActor2
    }

    "forward message to the child associated with the chat ID" in {
      val manager = system.actorOf(ChatThreadManager.props(500.millis, MockChatActor.props))

      manager.tell(StartRollCall(1, "title 1"), probe.ref)
      probe.expectMsg(StartRollCallResponse(1))
      val createdChatActor1 = probe.lastSender

      manager.tell(EndRollCall(1), probe.ref)
      probe.expectMsg(EndRollCallResponse(1))
      val existingChatActor1 = probe.lastSender

      existingChatActor1 shouldBe createdChatActor1
    }

    "automatically kill child actor after an idle period" in {
      val manager = system.actorOf(ChatThreadManager.props(500.millis, MockChatActor.props))

      manager.tell(StartRollCall(1, "title 1"), probe.ref)
      probe.expectMsg(StartRollCallResponse(1))
      val chatActor = probe.lastSender

      // to be notified when chatActor is terminated
      probe.watch(chatActor)

      Thread.sleep(600)
      probe.expectTerminated(chatActor)
    }
  }

  class MockChatActor(chatId: ChatId) extends Actor {
    override def receive: Receive = {
      case StartRollCall(`chatId`, _) =>
        sender() ! StartRollCallResponse(chatId)
      case EndRollCall(`chatId`) =>
        sender() ! EndRollCallResponse(chatId)
    }
  }

  object MockChatActor {
    def props(chatId: ChatId) = Props(new MockChatActor(chatId))
  }

}
