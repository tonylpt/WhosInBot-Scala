package com.whosin.telegram

import com.bot4s.telegram.api.TelegramBot
import com.bot4s.telegram.clients.AkkaHttpClient
import com.bot4s.telegram.methods.ParseMode.ParseMode
import com.bot4s.telegram.models._
import com.whosin.actors.BaseActorTest
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future

/**
  * @author tony
  */
class BotHelpersTest extends BaseActorTest("BotHelpersTest") with MockFactory {

  "addCommand" should {

    object fixture {
      val message = Message(
        messageId = 0,
        date = 1000,
        text = Some("/hello argument"),
        chat = Chat(id = 123L, `type` = ChatType.Group),
        from = Some(User(id = 456, firstName = "Peter", isBot = false))
      )
      val parsedCommand = ParsedCommand(123L, 456, "Peter", "argument", "/hello argument")
    }

    "add a command handler that automatically reply on success" in {
      val response = "command response"

      val commandHandler = stubFunction[ParsedCommand, Future[String]]
      commandHandler.when(*).returns(Future.successful(response))

      val replyFunc = stubFunction[String, Unit]
      replyFunc.when(*).returns(Unit)

      val bot = new MockAkkaBot(replyFunc)
      bot.addCommand('hello)(commandHandler)

      bot.receiveUpdate(Update(1L, message = Some(fixture.message)))
      Thread.sleep(50)

      commandHandler.verify(fixture.parsedCommand)
      replyFunc.verify(response)

      bot.shutdown()
    }

    "add a command handler that automatically reply on failure" in {
      val commandHandler = stubFunction[ParsedCommand, Future[String]]
      commandHandler.when(*).returns(Future.failed(new Exception("mock error, please ignore")))

      val replyFunc = stubFunction[String, Unit]
      replyFunc.when(*).returns(Unit)

      val bot = new MockAkkaBot(replyFunc)
      bot.addCommand('hello)(commandHandler)

      bot.receiveUpdate(Update(1L, message = Some(fixture.message)))
      Thread.sleep(50)

      commandHandler.verify(ParsedCommand(123L, 456, "Peter", "argument", "/hello argument"))
      replyFunc.verify("An error has occurred.")

      bot.shutdown()
    }
  }

  class MockAkkaBot(onReply: String => Unit)
    extends TelegramBot with BotHelpers {

    override val client = new AkkaHttpClient("token")

    override def reply(text: String, parseMode: Option[ParseMode],
                       disableWebPagePreview: Option[Boolean], disableNotification: Option[Boolean],
                       replyToMessageId: Option[Int], replyMarkup: Option[ReplyMarkup])
                      (implicit message: Message): Future[Message] = {
      onReply(text)
      Future.successful(message)
    }
  }

}

class ParsedCommandTest extends WordSpec with Matchers {

  "ParsedCommand.from" should {
    "parse Message with command argument" in {
      val message = Message(
        messageId = 0,
        date = 1000,
        text = Some("/command  cmd argument "),
        chat = Chat(id = 123L, `type` = ChatType.Group),
        from = Some(User(
          id = 456,
          firstName = "Peter",
          isBot = false,
        ))
      )

      val expected = ParsedCommand(123L, 456, "Peter", "cmd argument", "/command  cmd argument ")
      val actual = ParsedCommand.from(message).get

      actual shouldEqual expected
    }

    "parse Message without command argument" in {
      val message = Message(
        messageId = 0,
        date = 1000,
        text = Some("/command"),
        chat = Chat(id = 123L, `type` = ChatType.Group),
        from = Some(User(
          id = 456,
          firstName = "Peter",
          isBot = false,
        ))
      )

      val expected = ParsedCommand(123L, 456, "Peter", "", "/command")
      val actual = ParsedCommand.from(message).get

      actual shouldEqual expected
    }

    "parse Message with empty text" in {
      val message = Message(
        messageId = 0,
        date = 1000,
        text = Some(""),
        chat = Chat(id = 123L, `type` = ChatType.Group),
        from = Some(User(
          id = 456,
          firstName = "Peter",
          isBot = false,
        ))
      )

      ParsedCommand.from(message) shouldBe None
    }
  }
}
