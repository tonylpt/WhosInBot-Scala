package com.whosin.db

import java.sql.Timestamp

import com.whosin.db.Implicits._
import com.whosin.db.RollCallResponsesSupport.{Row, domainToRow, rowToDomain}
import com.whosin.db.profile.api._
import com.whosin.domain.Util.now
import com.whosin.domain.{AttendanceStatus, RollCallId, RollCallResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * @author tonyl
  */

trait RollCallResponseRepo {

  def getAll(callId: RollCallId): Future[List[RollCallResponse]]

  def addOrUpdate(response: RollCallResponse): Future[RollCallResponse]
}

class RollCallResponses(tag: Tag) extends Table[RollCallResponse](tag, "w_roll_call_responses") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def rollCallId = column[Long]("roll_call_id")

  def uniqueToken = column[String]("unique_token")

  def userId = column[Long]("user_id")

  def username = column[String]("user_name")

  def status = column[String]("status")

  def reason = column[String]("reason")

  def createdAt = column[Timestamp]("created_at")

  def updatedAt = column[Timestamp]("updated_at")

  def * =
    (id.?, rollCallId, uniqueToken, userId, username, status, reason, createdAt, updatedAt).shaped <>
      (rowToDomain, domainToRow)

  def rollCall = foreignKey("FK_ROLL_CALL_RESPONSES", rollCallId, rollCalls)(_.id)

}

object RollCallResponses extends RollCallResponseRepo {

  def getAll(callId: RollCallId): Future[List[RollCallResponse]] =
    database.run {
      rollCallResponses.filter(_.rollCallId === callId)
        .sortBy(_.updatedAt.asc)
        .result
    }.map(_.toList)

  def addOrUpdate(u: RollCallResponse): Future[RollCallResponse] = {
    val nowTs = now().toTimestamp
    database.run {
      // Note:
      // Commented out because it is not possible to update just
      // a subset of columns with Slick's insertOrUpdate.
      // rollCallResponses.insertOrUpdate(u.copy(updatedAt = now()))

      sql"""
          INSERT INTO w_roll_call_responses (
          roll_call_id, unique_token,
          user_id, user_name,
          status, reason,
          created_at, updated_at)

          VALUES (
          ${u.rollCallId}, ${u.uniqueToken},
          ${u.userId}, ${u.username},
          ${u.status.toString}, ${u.reason},
          $nowTs, $nowTs)

          ON CONFLICT (roll_call_id, unique_token)
          DO UPDATE SET
            user_id = ${u.userId},
            user_name = ${u.username},
            status = ${u.status.toString},
            reason = ${u.reason},
            updated_at = $nowTs

          RETURNING id, roll_call_id, unique_token,
                    user_id, user_name,
                    status, reason,
                    created_at, updated_at
        """.as[Row]

    }.map(_.head).map(rowToDomain)
  }
}

object RollCallResponsesSupport {
  type Row = (Option[Long], RollCallId, String, Long, String, String, String, Timestamp, Timestamp)

  def rowToDomain(row: Row): RollCallResponse = {
    val (id, rollCallId, uniqueToken, userId, username, status, reason, createdAt, updatedAt) = row
    RollCallResponse(
      id, userId, username,
      AttendanceStatus.withName(status), reason,
      uniqueToken, rollCallId,
      createdAt.toDate, updatedAt.toDate
    )
  }

  def domainToRow(c: RollCallResponse): Option[Row] = Some(
    c.id, c.rollCallId, c.uniqueToken,
    c.userId, c.username,
    c.status.toString, c.reason,
    c.createdAt.toTimestamp, c.updatedAt.toTimestamp
  )
}
