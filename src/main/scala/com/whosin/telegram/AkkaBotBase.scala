package com.whosin.telegram

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.api.{BotBase, BotExecutionContext, Polling, TelegramBot}
import com.bot4s.telegram.clients.AkkaHttpClient
import com.bot4s.telegram.models.Message
import com.whosin.domain._
import com.whosin.telegram.AkkaBotBaseSupport.CommandHandler
import slogging.StrictLogging

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * @author tonyl
  */

// $COVERAGE-OFF$

class AkkaBotBase(token: String,
                  implicit private val system: ActorSystem,
                  implicit private val materializer: ActorMaterializer)

  extends TelegramBot
    with Polling
    with AkkaBotBaseSupport {

  override val client = new AkkaHttpClient(token)
}

// $COVERAGE-ON$

object AkkaBotBaseSupport {
  type CommandHandler = ParsedCommand => Future[String]
}

trait AkkaBotBaseSupport extends Commands with StrictLogging {
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

  protected implicit def toFuture(string: String): Future[String] = {
    Future.successful(string)
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