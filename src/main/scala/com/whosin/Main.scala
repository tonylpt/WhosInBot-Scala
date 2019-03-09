package com.whosin

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import com.whosin.actors.ChatThreadManager
import com.whosin.telegram.WhosInBot

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
  * @author tonyl
  */

// $COVERAGE-OFF$

object Main extends App {

  private val app = new BotApp()

  Runtime.getRuntime.addShutdownHook(new Thread(() => app.stop()))

  app.run()
}

class BotApp extends StrictLogging {
  private val config = ConfigFactory.load()

  private val system = ActorSystem()
  private val materializer = ActorMaterializer()(system)

  private val manager = system.actorOf(ChatThreadManager.props(), "chat-manager")
  private val bot = new WhosInBot(config.getString("telegram.token"),
    system, materializer, manager)

  def run(): Unit = {
    val eol = bot.run()
    logger.info("Bot is started.")
    Await.result(eol, Duration.Inf)
  }

  def stop(): Unit = {
    logger.info("Shutting down...")

    bot.shutdown()
    logger.info("Bot is shut down.")

    materializer.shutdown()
    Await.result(system.terminate(), 5.second)
    logger.info("Actor system is shut down.")
  }
}

// $COVERAGE-ON$