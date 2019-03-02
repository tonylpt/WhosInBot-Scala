package com.whosin.telegram

import com.bot4s.telegram.models.{Chat, ChatType, Message, User}
import org.scalatest._

/**
  * @author tonyl
  */

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
