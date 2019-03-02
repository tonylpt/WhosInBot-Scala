package com.whosin.actors

import akka.actor.ActorSystem
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

  override final protected def afterAll(): Unit = {
    shutdown(system)
  }
}
