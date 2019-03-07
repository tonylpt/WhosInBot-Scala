package com.whosin.telegram

import akka.actor.{Actor, ActorRef, Props}
import com.bot4s.telegram.api.TelegramBot
import com.bot4s.telegram.clients.AkkaHttpClient
import com.bot4s.telegram.methods.ParseMode.ParseMode
import com.bot4s.telegram.models._
import com.whosin.actors.BaseActorTest
import com.whosin.actors.ChatCommands._
import com.whosin.domain.AttendanceStatus.{In, Maybe, Out}
import org.scalamock.function.FunctionAdapter1
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.Future

/**
  * @author tony
  */
class WhosInBotBaseTest extends BaseActorTest("WhosInBotBaseTest") with MockFactory {

  val chatId = 123L

  "WhosInBotBase" should {
    "handle /start_roll_call" in {
      val command = StartRollCall(chatId, "title")
      testWithExpectedCommand("/start_roll_call title", command)
    }

    "handle /end_roll_call" in {
      val command = EndRollCall(chatId)
      testWithExpectedCommand("/end_roll_call", command)
    }

    "handle /set_title with valid title" in {
      val command = UpdateTitle(chatId, "title")
      testWithExpectedCommand("/set_title title", command)
    }

    "handle /set_title with empty title" in {
      testWithExpectedReply("/set_title", "Please provide a title.")
    }

    "handle /shh" in {
      val command = UpdateQuiet(chatId, quiet = true)
      testWithExpectedCommand("/shh", command)
    }

    "handle /louder" in {
      val command = UpdateQuiet(chatId, quiet = false)
      testWithExpectedCommand("/louder", command)
    }

    "handle /in" in {
      val command = UpdateAttendanceSelf(chatId, 456, "Peter", In, "will come")
      testWithExpectedCommand("/in will come", command)
    }

    "handle /out" in {
      val command = UpdateAttendanceSelf(chatId, 456, "Peter", Out, "won't come")
      testWithExpectedCommand("/out won't come", command)
    }

    "handle /maybe" in {
      val command = UpdateAttendanceSelf(chatId, 456, "Peter", Maybe, "might come")
      testWithExpectedCommand("/maybe might come", command)
    }

    "handle /set_in_for" in {
      val command = UpdateAttendanceFor(chatId, "David", In, "will come")
      testWithExpectedCommand("/set_in_for David will come", command)
    }

    "handle /set_out_for" in {
      val command = UpdateAttendanceFor(chatId, "David", Out, "won't come")
      testWithExpectedCommand("/set_out_for David won't come", command)
    }

    "handle /set_maybe_for" in {
      val command = UpdateAttendanceFor(chatId, "David", Maybe, "might come")
      testWithExpectedCommand("/set_maybe_for David might come", command)
    }

    "handle /whos_in" in {
      val command = GetAllAttendance(chatId)
      testWithExpectedCommand("/whos_in", command)
    }

    "handle /available_commands" in {
      testWithExpectedReply("/available_commands", where[String] { reply =>
        reply.contains("start_roll_call") && reply.contains("end_roll_call")
      })
    }
  }

  def testWithExpectedCommand(text: String, expectedCommand: Command): Unit = {
    testWithExpectedReply(text, expectedCommand.toString)
  }

  def testWithExpectedReply(text: String, expectedReply: String): Unit = {
    testWithExpectedReply(text, where[String] { arg =>
      arg == expectedReply
    })
  }

  def testWithExpectedReply(text: String, replyAssertion: FunctionAdapter1[String, Boolean]): Unit = {
    val message = Message(
      messageId = 0,
      date = 1000,
      text = Some(text),
      chat = Chat(id = chatId, `type` = ChatType.Group),
      from = Some(User(id = 456, firstName = "Peter", isBot = false))
    )

    val managerActor = system.actorOf(Props(new EchoActor))

    val replyFunc = stubFunction[String, Unit]
    replyFunc.when(*).returns(Unit)

    val bot = new SimpleWhosInBot(replyFunc, managerActor)
    bot.receiveUpdate(Update(1L, message = Some(message)))
    bot.shutdown()
    Thread.sleep(50)

    replyFunc.verify(replyAssertion)
  }


  class SimpleWhosInBot(onReply: String => Unit,
                        override val managerActor: ActorRef)

    extends TelegramBot with WhosInBotBase {

    override val client = new AkkaHttpClient("token")

    override def reply(text: String, parseMode: Option[ParseMode],
                       disableWebPagePreview: Option[Boolean], disableNotification: Option[Boolean],
                       replyToMessageId: Option[Int], replyMarkup: Option[ReplyMarkup])
                      (implicit message: Message): Future[Message] = {
      onReply(text)
      Future.successful(message)
    }

    override protected def renderResponse(response: Any): String = response.toString
  }

  /**
    * An actor that echo whatever message it receives.
    * This is to verify that the bot send the correct message to the actor.
    */
  class EchoActor extends Actor {
    override def receive: Receive = {
      case message =>
        sender() ! message
    }
  }

}

class NameAndReasonTest extends WordSpecLike with Matchers {

  "unapply" should {
    "extract name and reason when both are available" in {
      val result = "Peter  will be busy " match {
        case NameAndReason(name, reason) => Some((name, reason))
        case _ => None
      }

      result shouldEqual Some(("Peter", "will be busy"))
    }

    "extract name and reason when name is available but reason is empty" in {
      val result = "Peter" match {
        case NameAndReason(name, reason) => Some((name, reason))
        case _ => None
      }

      result shouldEqual Some(("Peter", ""))
    }

    "return None when command is not valid" in {
      val result = "" match {
        case NameAndReason(name, reason) => Some((name, reason))
        case _ => None
      }

      result shouldBe None
    }
  }
}
