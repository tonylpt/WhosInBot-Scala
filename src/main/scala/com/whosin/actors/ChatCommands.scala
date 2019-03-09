package com.whosin.actors

import com.whosin.domain._

/**
  * @author tony
  */

object ChatCommands {

  sealed trait Command {
    def chatId: ChatId
  }

  sealed trait CommandResponse {
    def chatId: ChatId
  }

  case class StartRollCall(chatId: ChatId, title: String) extends Command

  case class StartRollCallResponse(chatId: ChatId) extends CommandResponse


  case class EndRollCall(chatId: ChatId) extends Command

  case class EndRollCallResponse(chatId: ChatId) extends CommandResponse


  case class UpdateTitle(chatId: ChatId, title: String) extends Command

  case class UpdateTitleResponse(chatId: ChatId, rollCall: RollCall) extends CommandResponse


  case class UpdateQuiet(chatId: ChatId, quiet: Boolean) extends Command

  case class UpdateQuietResponse(chatId: ChatId,
                                 rollCall: RollCall,
                                 responses: List[RollCallResponse]) extends CommandResponse


  case class UpdateAttendanceSelf(chatId: ChatId,
                                  userId: UserId,
                                  username: String,
                                  status: AttendanceStatus,
                                  reason: String) extends Command

  case class UpdateAttendanceSelfResponse(chatId: ChatId,
                                          username: String,
                                          status: AttendanceStatus,
                                          rollCall: RollCall,
                                          responses: List[RollCallResponse]) extends CommandResponse


  case class UpdateAttendanceFor(chatId: ChatId,
                                 username: String,
                                 status: AttendanceStatus,
                                 reason: String) extends Command

  case class UpdateAttendanceForResponse(chatId: ChatId,
                                         username: String,
                                         status: AttendanceStatus,
                                         rollCall: RollCall,
                                         responses: List[RollCallResponse]) extends CommandResponse


  case class GetAllAttendance(chatId: ChatId) extends Command

  case class GetAllAttendanceResponse(chatId: ChatId,
                                      rollCall: RollCall,
                                      responses: List[RollCallResponse]) extends CommandResponse


  case class NoActiveRollCallResponse(chatId: ChatId) extends CommandResponse

  case class CommandFailure(chatId: ChatId, error: Throwable) extends CommandResponse

}
