package com.whosin.db

import java.sql.Timestamp

import com.whosin.db.Implicits._
import com.whosin.db.profile.api._
import com.whosin.db.slickext.UpdateReturning._
import com.whosin.domain.{ChatId, RollCall, RollCallId, RollCallStatus}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * @author tonyl
  */

trait RollCallRepo {

  def insertAndCleanUp(chatId: ChatId, title: String): Future[RollCall]

  def closeCurrent(chatId: ChatId): Future[Boolean]

  def getCurrent(chatId: ChatId): Future[Option[RollCall]]

  def setCurrentTitle(chatId: ChatId, title: String): Future[Option[RollCall]]

  def setCurrentQuiet(chatId: ChatId, quiet: Boolean): Future[Option[RollCall]]
}

class RollCalls(tag: Tag) extends Table[RollCall](tag, "w_roll_calls") {

  def id = column[RollCallId]("id", O.PrimaryKey, O.AutoInc)

  def chatId = column[Long]("chat_id")

  def status = column[String]("status")

  def title = column[String]("title")

  def quiet = column[Boolean]("quiet")

  def createdAt = column[Timestamp]("created_at")

  def updatedAt = column[Timestamp]("updated_at")

  def * = {
    (id.?, chatId, status, title, quiet, createdAt, updatedAt).shaped <>
      ( {
        case (id, chatId, status, title, quiet, createdAt, updatedAt) =>
          RollCall(id, chatId, RollCallStatus.withName(status), title, quiet, createdAt.toDate, updatedAt.toDate)
      }, { c: RollCall =>
        Some((c.id, c.chatId, c.status.entryName, c.title, c.quiet,
          c.createdAt.toTimestamp, c.updatedAt.toTimestamp))
      })
  }
}

object RollCalls extends RollCallRepo {

  val CleanupThreshold: Int = 10

  private implicit val db: Database = database

  private def openCallQuery(chatId: ChatId) = {
    rollCalls.filter { row =>
      row.chatId === chatId &&
        row.status === RollCallStatus.Open.entryName
    }
  }

  private def closeAllCalls(chatId: ChatId) = {
    val status = openCallQuery(chatId).map(_.status)
    status.update(RollCallStatus.Closed.entryName)
  }

  private def deleteOldCalls(chatId: ChatId, numToKeep: Int) = {
    val latestKeepRows = rollCalls
      .filter { row =>
        row.chatId === chatId && row.status === RollCallStatus.Closed.entryName
      }
      .sortBy(_.createdAt.desc)
      .take(numToKeep)
    val earliestCreatedAt = latestKeepRows
      .map(_.createdAt)
      .min
    rollCalls
      .filter { row =>
        row.chatId === chatId && row.createdAt < earliestCreatedAt
      }
      .delete
  }

  private def addCall(rollCall: RollCall) = {
    (rollCalls returning rollCalls.map(_.id)) += rollCall.copy(id = None)
  }

  def insertAndCleanUp(chatId: ChatId, title: String): Future[RollCall] = {
    val call = RollCall(None, chatId, RollCallStatus.Open, title, quiet = false)
    val actions = closeAllCalls(chatId)
      .andThen(deleteOldCalls(chatId, CleanupThreshold))
      .andThen(addCall(call))

    database.run(actions).map { id =>
      call.copy(id = Some(id))
    }
  }

  def closeCurrent(chatId: ChatId): Future[Boolean] = {
    database.run(closeAllCalls(chatId)).map(_ > 0)
  }

  def getCurrent(chatId: ChatId): Future[Option[RollCall]] =
    database.run {
      openCallQuery(chatId)
        .sortBy(_.updatedAt.desc)
        .take(1)
        .result
        .headOption
    }

  def setCurrentTitle(chatId: ChatId, title: String): Future[Option[RollCall]] =
    database.run {
      openCallQuery(chatId)
        .map(_.title)
        .updateReturning(rollCalls, title)
        .headOption
    }

  def setCurrentQuiet(chatId: ChatId, quiet: Boolean): Future[Option[RollCall]] =
    database.run {
      openCallQuery(chatId)
        .map(_.quiet)
        .updateReturning(rollCalls, quiet)
        .headOption
    }
}
