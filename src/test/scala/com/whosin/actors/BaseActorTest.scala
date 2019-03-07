package com.whosin.actors

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpecLike}

/**
  * @author tony
  */

abstract class BaseActorTest(name: String) extends TestKit(ActorSystem(name))
  with Matchers
  with WordSpecLike
  with BeforeAndAfter
  with BeforeAndAfterAll {

  protected implicit val materializer: ActorMaterializer = ActorMaterializer()

  override final protected def afterAll(): Unit = {
    materializer.shutdown()
    shutdown(system)
  }
}
