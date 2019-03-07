package com.whosin.telegram

import com.whosin.telegram.WhosInBotSupport.NameAndReason
import org.scalatest.{Matchers, WordSpecLike}

/**
  * @author tony
  */
class WhosInBotTest extends WordSpecLike with Matchers {

}

class WhosInBotSupportTest extends WordSpecLike with Matchers {

  "NameAndReason.unapply" should {
    "extract name and reason when both are available" in {
      val result = "Peter  will be busy " match {
        case NameAndReason(name, reason) => Some((name, reason))
        case _ => None
      }

      result shouldEqual Some(("Peter", "will be busy"))
    }

    "extract name and reason when name is available but reason is empty" in {
      val result = "Peter" match {
        case NameAndReason(name, reason) => Some((name, reason))
        case _ => None
      }

      result shouldEqual Some(("Peter", ""))
    }

    "return None when command is not valid" in {
      val result = "" match {
        case NameAndReason(name, reason) => Some((name, reason))
        case _ => None
      }

      result shouldBe None
    }
  }
}
