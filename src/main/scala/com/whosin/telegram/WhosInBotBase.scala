package com.whosin.telegram

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import com.bot4s.telegram.api.{BotBase, BotExecutionContext}
import com.whosin.actors.ChatCommands._
import com.whosin.domain.AttendanceStatus.{AttendanceStatus, In, Maybe, Out}
import com.whosin.telegram.BotHelpers.CommandHandler

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

/**
  * @author tony
  */

trait WhosInBotBase extends BotHelpers with WhosInBotHandlers {
  this: BotBase with BotExecutionContext =>

  addCommand('start_roll_call)(startRollCall)
  addCommand('end_roll_call)(endRollCall)

  addCommand('set_title)(setTitle)
  addCommand('shh)(setQuiet(true))
  addCommand('louder)(setQuiet(false))

  addCommand('in)(setAttendanceFn(In))
  addCommand('out)(setAttendanceFn(Out))
  addCommand('maybe)(setAttendanceFn(Maybe))
  addCommand('set_in_for)(setAttendanceForFn(In))
  addCommand('set_out_for)(setAttendanceForFn(Out))
  addCommand('set_maybe_for)(setAttendanceForFn(Maybe))

  addCommand('whos_in)(listResponses)
  addCommand('available_commands)(listCommands)

}

trait WhosInBotHandlers {

  def managerActor: ActorRef

  def executionContext: ExecutionContext

  def process(command: Command): Future[String] = {
    implicit val ec: ExecutionContext = this.executionContext
    implicit val timeout: Timeout = 60.second

    for {
      response <- this.managerActor ? command
      replyMsg = renderResponse(response)
    } yield replyMsg
  }

  protected def renderResponse(response: Any): String = {
    View.getReplyText(response.asInstanceOf[CommandResponse])
  }

  def startRollCall: CommandHandler = { cmd =>
    process(StartRollCall(cmd.chatId, cmd.commandArg))
  }

  def endRollCall: CommandHandler = { cmd =>
    process(EndRollCall(cmd.chatId))
  }

  def listResponses: CommandHandler = { cmd =>
    process(GetAllAttendance(cmd.chatId))
  }

  def setTitle: CommandHandler = { cmd =>
    cmd.commandArg match {
      case title if title.isEmpty =>
        "Please provide a title."
      case title =>
        process(UpdateTitle(cmd.chatId, title))
    }
  }

  def setQuiet(quiet: Boolean): CommandHandler = { cmd =>
    process(UpdateQuiet(cmd.chatId, quiet = quiet))
  }

  def setAttendanceFn(status: AttendanceStatus): CommandHandler = { cmd =>
    process(UpdateAttendanceSelf(cmd.chatId, cmd.userId, cmd.username, status, cmd.commandArg))
  }

  def setAttendanceForFn(status: AttendanceStatus): CommandHandler = { cmd =>
    cmd.commandArg match {
      case NameAndReason(name, reason) =>
        process(UpdateAttendanceFor(cmd.chatId, name, status, reason))
      case _ =>
        "Please provide the person's name."
    }
  }

  def listCommands: CommandHandler = { _ =>
    View.getAvailableCommands(List(
      "start_roll_call",
      "end_roll_call",
      "set_title",
      "shh",
      "louder",
      "in",
      "out",
      "maybe",
      "set_in_for",
      "set_out_for",
      "set_maybe_for",
      "whos_in",
      "available_commands"
    ))
  }

  protected implicit def toFuture(string: String): Future[String] = {
    Future.successful(string)
  }
}


/** Encapsulates name and reason for setting attendance on behalf of someone else. */
case class NameAndReason(name: String, reason: String)

case object NameAndReason {
  private val regex = """^(\S+)\s*(.*)$""".r

  def unapply(arg: String): Option[(String, String)] = arg match {
    case regex(name, reason) => Some((name, reason.trim))
    case _ => None
  }
}
