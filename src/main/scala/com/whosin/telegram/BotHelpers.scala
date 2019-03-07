package com.whosin.telegram

import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.api.{BotBase, BotExecutionContext}
import com.bot4s.telegram.models.Message
import com.whosin.domain._
import com.whosin.telegram.BotHelpers.{CommandHandler}
import slogging.StrictLogging

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * @author tonyl
  */

object BotHelpers {
  type CommandHandler = ParsedCommand => Future[String]
}

trait BotHelpers extends Commands with StrictLogging {
  this: BotBase with BotExecutionContext =>

  def addCommand(s: Symbol)(handler: CommandHandler): Unit = {
    onCommand(s) { implicit msg =>
      ParsedCommand.from(msg) match {
        case None =>
          logger.error("Failed to parse command: {}", msg)
        case Some(parsedCommand) =>
          handler(parsedCommand).onComplete {
            case Success(response) =>
              reply(response)

            case Failure(exception) =>
              logger.error("An error has occurred.", exception)
              reply("An error has occurred.")
          }
      }
    }
  }
}


case class ParsedCommand(chatId: ChatId,
                         userId: UserId,
                         username: String,
                         commandArg: String,
                         text: String)

object ParsedCommand {

  private val commandArgRegex = """^/\S+\s*(.*)$""".r

  def from(message: Message): Option[ParsedCommand] = {
    for {
      text <- message.text
      commandArg <- text match {
        case commandArgRegex(arg) => Some(arg.trim)
        case _ => None
      }

      user <- message.from
      userId = user.id
      username = user.firstName
      chatId = message.chat.id
    } yield ParsedCommand(chatId, userId, username, commandArg, text)
  }
}
