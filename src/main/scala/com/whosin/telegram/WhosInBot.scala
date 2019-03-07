package com.whosin.telegram

import akka.actor._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.whosin.actors.ChatCommands._
import com.whosin.domain.AttendanceStatus._
import com.whosin.telegram.AkkaBotBaseSupport.CommandHandler
import com.whosin.telegram.WhosInBotSupport.NameAndReason

import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * @author tonyl
  */

class WhosInBot(token: String,
                system: ActorSystem,
                materializer: ActorMaterializer,
                val managerActor: ActorRef)
  extends AkkaBotBase(token, system, materializer) with WhosInBotSupport {

  addCommand('start_roll_call) { cmd =>
    process(StartRollCall(cmd.chatId, cmd.commandArg))
  }

  addCommand('end_roll_call) { cmd =>
    process(EndRollCall(cmd.chatId))
  }

  addCommand('set_title) { cmd =>
    cmd.commandArg match {
      case title if title.isEmpty =>
        "Please provide a title."
      case title =>
        process(UpdateTitle(cmd.chatId, title))
    }
  }

  addCommand('shh) { cmd =>
    process(UpdateQuiet(cmd.chatId, quiet = true))
  }

  addCommand('louder) { cmd =>
    process(UpdateQuiet(cmd.chatId, quiet = false))
  }

  addCommand('whos_in) { cmd =>
    process(GetAllAttendance(cmd.chatId))
  }

  addCommand('in)(setAttendanceFn(In))
  addCommand('out)(setAttendanceFn(Out))
  addCommand('maybe)(setAttendanceFn(Maybe))

  addCommand('set_in_for)(setAttendanceForFn(In))
  addCommand('set_out_for)(setAttendanceForFn(Out))
  addCommand('set_maybe_for)(setAttendanceForFn(Maybe))

  addCommand('available_commands) { _ =>
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

}

private[telegram] trait WhosInBotSupport {
  this: WhosInBot =>

  def process(command: Command): Future[String] = {
    implicit val timeout: Timeout = 60.second
    for {
      response <- this.managerActor ? command
      replyMsg = View.getReplyText(response.asInstanceOf[CommandResponse])
    } yield replyMsg
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

}

object WhosInBotSupport {

  /** Encapsulates name and reason for setting attendance on behalf of someone else. */
  case class NameAndReason(name: String, reason: String)

  case object NameAndReason {
    private val regex = """^(\S+)\s*(.*)$""".r

    def unapply(arg: String): Option[(String, String)] = arg match {
      case regex(name, reason) => Some((name, reason.trim))
      case _ => None
    }
  }

}