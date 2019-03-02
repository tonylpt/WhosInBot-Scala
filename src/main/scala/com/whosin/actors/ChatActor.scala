package com.whosin.actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.pipe
import com.whosin.actors.ChatCommands._
import com.whosin.db.{RollCallRepo, RollCallResponseRepo, RollCallResponses, RollCalls}
import com.whosin.domain.AttendanceStatus.AttendanceStatus
import com.whosin.domain._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * @author tonyl
  */

object ChatActor {

  def props(chatId: ChatId): Props = props(chatId, RollCalls, RollCallResponses)

  def props(chatId: ChatId,
            rollCallRepo: RollCallRepo,
            rollCallResponseRepo: RollCallResponseRepo): Props =
    Props(new ChatActor(chatId, rollCallRepo, rollCallResponseRepo))

}

//noinspection ActorMutableStateInspection
class ChatActor(override val chatId: ChatId,
                override val rollCallRepo: RollCallRepo,
                override val rollCallResponseRepo: RollCallResponseRepo)
  extends Actor with ActorLogging with ChatActorSupport {

  override def receive: Receive = {
    case StartRollCall(`chatId`, title) =>
      handleStartRollCall(title).pipeTo(sender)

    case EndRollCall(`chatId`) =>
      handleEndRollCall().pipeTo(sender)

    case UpdateTitle(`chatId`, title) =>
      handleUpdateTitle(title).pipeTo(sender)

    case UpdateQuiet(`chatId`, quiet) =>
      handleUpdateQuiet(quiet).pipeTo(sender)

    case UpdateAttendanceSelf(`chatId`, userId, username, status, reason) =>
      handleUpdateAttendanceSelf(userId, username, status, reason).pipeTo(sender)

    case UpdateAttendanceFor(`chatId`, username, status, reason) =>
      handleUpdateAttendanceFor(username, status, reason).pipeTo(sender)

    case GetAllAttendance(`chatId`) =>
      handleGetAllAttendance().pipeTo(sender)

    case unknown =>
      log.error("Unrecognized message: {}", unknown)
  }

}

private[actors] trait ChatActorSupport {
  this: ChatActor =>

  def chatId: ChatId

  protected def rollCallRepo: RollCallRepo

  protected def rollCallResponseRepo: RollCallResponseRepo

  def handleError: PartialFunction[Throwable, CommandResponse] = {
    case exception =>
      log.error(exception, "An error has occurred.")
      CommandFailure(chatId, exception)
  }

  def handleStartRollCall(title: String): Future[CommandResponse] = {
    rollCallRepo.insertAndCleanUp(chatId, title).map { _ =>
      StartRollCallResponse(chatId)
    }.recover(handleError)
  }

  def handleEndRollCall(): Future[CommandResponse] = {
    rollCallRepo.closeCurrent(chatId).map {
      case false => NoActiveRollCallResponse(chatId)
      case true => EndRollCallResponse(chatId)
    }.recover(handleError)
  }

  def handleUpdateTitle(title: String): Future[CommandResponse] = {
    rollCallRepo.setCurrentTitle(chatId, title).map {
      case None => NoActiveRollCallResponse(chatId)
      case Some(call) => UpdateTitleResponse(chatId, call)
    }.recover(handleError)
  }

  def handleUpdateQuiet(quiet: Boolean): Future[CommandResponse] = {
    rollCallRepo.setCurrentQuiet(chatId, quiet).flatMap {
      case None => Future.successful(NoActiveRollCallResponse(chatId))
      case Some(call) =>
        rollCallResponseRepo.getAll(call.id.get).map { responses =>
          UpdateQuietResponse(chatId, call, responses)
        }
    }.recover(handleError)
  }

  def handleUpdateAttendanceSelf(userId: UserId,
                                 username: String,
                                 status: AttendanceStatus,
                                 reason: String): Future[CommandResponse] = {
    rollCallRepo.getCurrent(chatId).flatMap {
      case None =>
        Future.successful(NoActiveRollCallResponse(chatId))
      case Some(call) =>
        val response = RollCallResponse.createSelfResponse(userId, username, status, reason, call.id.get)
        for {
          _ <- rollCallResponseRepo.addOrUpdate(response)
          responses <- rollCallResponseRepo.getAll(call.id.get)
        } yield UpdateAttendanceSelfResponse(chatId, username, status, call, responses)
    }.recover(handleError)
  }

  def handleUpdateAttendanceFor(username: String,
                                status: AttendanceStatus,
                                reason: String): Future[CommandResponse] = {
    rollCallRepo.getCurrent(chatId).flatMap {
      case None => Future.successful(NoActiveRollCallResponse(chatId))
      case Some(call) =>
        val response = RollCallResponse.createForResponse(username, status, reason, call.id.get)
        for {
          _ <- rollCallResponseRepo.addOrUpdate(response)
          responses <- rollCallResponseRepo.getAll(call.id.get)
        } yield UpdateAttendanceForResponse(chatId, username, status, call, responses)
    }.recover(handleError)
  }

  def handleGetAllAttendance(): Future[CommandResponse] = {
    rollCallRepo.getCurrent(chatId).flatMap {
      case None => Future.successful(NoActiveRollCallResponse(chatId))
      case Some(call) =>
        rollCallResponseRepo.getAll(call.id.get).map { responses =>
          GetAllAttendanceResponse(chatId, call, responses)
        }
    }.recover(handleError)
  }
}
