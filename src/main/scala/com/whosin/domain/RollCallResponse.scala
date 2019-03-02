package com.whosin.domain

import java.util.Date

import com.roundeights.hasher.Implicits._
import com.whosin.domain.AttendanceStatus.AttendanceStatus
import com.whosin.domain.Util._

/**
  * @author tonyl
  */

case class RollCallResponse(id: Option[Long],
                            userId: Long,
                            username: String,
                            status: AttendanceStatus,
                            reason: String,
                            uniqueToken: String,
                            rollCallId: Long,
                            createdAt: Date = now(),
                            updatedAt: Date = now())

object RollCallResponse {

  def createSelfResponse(userId: Long,
                         username: String,
                         status: AttendanceStatus,
                         reason: String,
                         rollCallId: Long): RollCallResponse = {

    val userHash = userId.toString.sha256
    val uniqueToken = s"self:$userHash"
    RollCallResponse(None, userId, username, status, reason, uniqueToken, rollCallId)
  }

  def createForResponse(username: String,
                        status: AttendanceStatus,
                        reason: String,
                        rollCallId: Long): RollCallResponse = {

    val userHash = username.toLowerCase.sha256
    val uniqueToken = s"for:$userHash"
    RollCallResponse(None, 0, username, status, reason, uniqueToken, rollCallId)
  }
}