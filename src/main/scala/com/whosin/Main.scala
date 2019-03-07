package com.whosin

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import com.whosin.actors.ChatThreadManager
import com.whosin.telegram.WhosInBot

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * @author tonyl
  */

// $COVERAGE-OFF$

object Main extends App with StrictLogging {
  val config = ConfigFactory.load()

  implicit val system: ActorSystem = ActorSystem()
  val materializer = ActorMaterializer()

  val manager = system.actorOf(ChatThreadManager.props(), "chat-manager")
  val bot = new WhosInBot(config.getString("telegram.token"), system, materializer, manager)
  val eol = bot.run()

  Runtime.getRuntime.addShutdownHook(new Thread {
    override def run(): Unit = bot.shutdown()
  })

  logger.info("Bot is started.")
  Await.result(eol, Duration.Inf)
}

// $COVERAGE-ON$