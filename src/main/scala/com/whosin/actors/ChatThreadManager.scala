package com.whosin.actors

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, PoisonPill, Props}
import com.whosin.actors.AutoCleanup.CleanupRequested
import com.whosin.actors.ChatCommands.Command
import com.whosin.domain.ChatId

import scala.concurrent.duration.{FiniteDuration, _}

/**
  * @author tonyl
  */

object ChatThreadManager {
  def props(maxIdle: FiniteDuration = 60.second,
            childPropsFactory: ChatId => Props = ChatActor.props): Props =
    Props(new ChatThreadManager(maxIdle, childPropsFactory))
}

/**
  * An actor that automatically creates a child actor for each chat ID, forward
  * the relevant [[Command]] to it, and delete the child actor after it has been idle
  * for a [[maxIdle]] duration.
  */
class ChatThreadManager(maxIdle: FiniteDuration, childPropsFactory: ChatId => Props)
  extends Actor with ActorLogging {

  private def receiveCommand(chatIdToActor: Map[ChatId, ActorRef],
                             actorToChatId: Map[ActorRef, ChatId]): Receive = {

    case message: Command =>
      val chatId = message.chatId
      chatIdToActor.get(chatId) match {
        case Some(actorRef) =>
          actorRef.forward(message)

        case None =>
          log.info("Creating auto-stopping actor for chat {}", chatId)
          val actorRef = context.actorOf(AutoCleanup.props(childPropsFactory(chatId), maxIdle), s"chat-$chatId")
          actorRef.forward(message)

          val newState = receiveCommand(
            chatIdToActor = chatIdToActor + (chatId -> actorRef),
            actorToChatId = actorToChatId + (actorRef -> chatId)
          )
          context.become(newState, discardOld = true)
      }

    case CleanupRequested(actorRef) =>
      val chatId = actorToChatId(actorRef)

      actorRef ! PoisonPill
      log.info("Actor for chat {} has been terminated", chatId)

      val newState = receiveCommand(
        chatIdToActor = chatIdToActor - chatId,
        actorToChatId = actorToChatId - actorRef
      )
      context.become(newState, discardOld = true)
  }

  override def receive: Receive = receiveCommand(Map.empty, Map.empty)
}

private object AutoCleanup {
  def props(actorProps: => Props, duration: FiniteDuration) =
    Props(new AutoCleanup(actorProps, duration))

  case class CleanupRequested(actor: ActorRef)

}

/**
  * An actor that forward all messages to a child actor that it creates
  * from [[actorProps]] and automatically sends a [[CleanupRequested]] message
  * to the parent actor after a [[maxIdle]] duration.
  */
private class AutoCleanup(actorProps: Props, maxIdle: FiniteDuration)
  extends Actor with ActorLogging {

  private val actor = context.actorOf(actorProps)

  private def scheduleNext(): Receive = {
    import context.dispatcher
    val nextUuid = UUID.randomUUID().toString
    val scheduled = context.system.scheduler.scheduleOnce(maxIdle, self, Check(nextUuid))
    receive(nextUuid, scheduled)
  }

  private def receive(uuid: String, ongoing: Cancellable): Receive = {
    case Check(`uuid`) =>
      context.parent ! CleanupRequested(self)
    case Check(_) =>
    // ignored
    case message =>
      actor.forward(message)
      ongoing.cancel()
      context.become(scheduleNext(), discardOld = true)
  }

  override def receive: Receive = scheduleNext()

  private case class Check(uuid: String)

}
