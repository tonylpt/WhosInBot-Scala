package com.whosin.telegram

import akka.actor._
import akka.stream.ActorMaterializer
import com.bot4s.telegram.api.{Polling, TelegramBot}
import com.bot4s.telegram.clients.AkkaHttpClient

/**
  * @author tonyl
  */

// $COVERAGE-OFF$

class WhosInBot(token: String,
                implicit private val system: ActorSystem,
                implicit private val materializer: ActorMaterializer,
                override val managerActor: ActorRef)

  extends TelegramBot
    with Polling
    with WhosInBotBase {

  override val client = new AkkaHttpClient(token)
}

// $COVERAGE-ON$
