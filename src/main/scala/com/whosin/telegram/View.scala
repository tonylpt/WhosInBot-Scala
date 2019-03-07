package com.whosin.telegram

import java.time.{LocalDateTime, ZoneOffset}

import com.whosin.actors.ChatCommands._
import com.whosin.domain.AttendanceStatus._
import com.whosin.domain.{RollCall, RollCallResponse}

/**
  * @author tonyl
  */

object View {

  implicit val localDateTimeOrdering: Ordering[LocalDateTime] =
    Ordering.by(_.toInstant(ZoneOffset.UTC).toEpochMilli)

  def getAvailableCommands(commands: List[String]): String = {
    ("Available commands:" +:
      commands.map(s => s" \uD83C\uDF7A /$s")).mkString("\n")
  }

  def getReplyText(r: CommandResponse): String = r match {
    // Note: exhaustive check will be disabled when there is a guard

    case _: CommandFailure =>
      "An error has occurred. Please try again later."

    case NoActiveRollCallResponse(_) =>
      "No roll call in progress."

    case StartRollCallResponse(_) =>
      "Roll call started."

    case EndRollCallResponse(_) =>
      "Roll call ended."

    case UpdateTitleResponse(_, _) =>
      "Roll call title set."

    case UpdateQuietResponse(_, call, responses) =>
      if (call.quiet) {
        "Ok fine, I'll be quiet. \uD83E\uDD10"
      } else {
        s"Sure. \uD83D\uDE03\n\n${listResponses(call, responses)}"
      }

    case UpdateAttendanceSelfResponse(_, username, status, call, responses) =>
      s"${announceAttendance(username, status)}\n\n${listResponses(call, responses)}"

    case UpdateAttendanceForResponse(_, username, status, call, responses) =>
      s"${announceAttendance(username, status)}\n\n${listResponses(call, responses)}"

    case GetAllAttendanceResponse(_, call, responses) =>
      withTitle(call, listResponses(call, responses))
  }

  private def announceAttendance(username: String, attendance: AttendanceStatus): String = attendance match {
    case In => s"$username is in!"
    case Out => s"$username is out!"
    case Maybe => s"$username might come!"
    case _ => throw new MatchError(s"Invalid attendance value: $attendance")
  }

  private def withTitle(rollCall: RollCall, body: String): String = {
    List(rollCall.title, body)
      .filterNot(_.isEmpty)
      .mkString("\n\n")
  }

  private def listResponses(rollCall: RollCall, responses: List[RollCallResponse]): String = rollCall match {
    case call if call.quiet => listResponsesShort(call, responses)
    case call => listResponsesFull(call, responses)
  }

  private def listResponsesShort(call: RollCall, responses: List[RollCallResponse]): String = {
    val countByStatus = responses
      .groupBy(_.status)
      .mapValues(_.size)

    val in = countByStatus.getOrElse(In, 0)
    val out = countByStatus.getOrElse(Out, 0)
    val maybe = countByStatus.getOrElse(Maybe, 0)

    s"Total: $in in, $out out, $maybe might come."
  }

  private def listResponsesFull(call: RollCall, responses: List[RollCallResponse]): String = {
    def getResponseLine(response: RollCallResponse): String = {
      val reason = response.reason
      if (reason.isEmpty)
        s" - ${response.username}"
      else
        s" - ${response.username} ($reason)"
    }

    def getStatusLine(status: AttendanceStatus, count: Int): String = status match {
      case In => s"In ($count)"
      case Out => s"Out ($count)"
      case Maybe => s"Maybe ($count)"
    }

    val attendanceByStatus = responses
      .groupBy(_.status)
      .mapValues(_.sortBy(_.updatedAt))

    val resultByStatus = for {
      status <- Array(In, Out, Maybe)
      attendances = attendanceByStatus.getOrElse(status, List.empty)
      count = attendances.size if count > 0
      attendancesStr = attendances.map(getResponseLine).mkString("\n")
      statusLine = getStatusLine(status, count)
    } yield s"$statusLine\n$attendancesStr"

    if (resultByStatus.isEmpty)
      "No responses yet. \uD83D\uDE22"
    else
      resultByStatus.mkString("\n\n")
  }
}
