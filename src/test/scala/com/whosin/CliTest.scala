package com.whosin

import com.whosin.RunConfig.{App, Migrate}
import org.scalatest.{Matchers, WordSpec}

/**
  * @author tony
  */
class CliTest extends WordSpec with Matchers {

  "parse" should {
    "return Migrate RunConfig when passed migrate command" in {
      val actual: Option[RunConfig] = Cli.parse(Array("migrate"))
      val expected: RunConfig = Migrate

      actual shouldEqual Some(expected)
    }

    "return App RunConfig when not passed any command" in {
      val actual: Option[RunConfig] = Cli.parse(Array.empty)
      val expected: RunConfig = App

      actual shouldEqual Some(expected)
    }

    "return None when passed invalid command" in {
      val actual: Option[RunConfig] = Cli.parse(Array("invalid"))

      actual shouldBe None
    }

    "return None when passed invalid option" in {
      val actual: Option[RunConfig] = Cli.parse(Array("--invalid"))

      actual shouldBe None
    }
  }
}
