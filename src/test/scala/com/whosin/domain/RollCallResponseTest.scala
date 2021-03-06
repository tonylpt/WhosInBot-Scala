package com.whosin.domain

import com.whosin.domain.AttendanceStatus.In
import org.scalatest.{Matchers, WordSpec}

/**
  * @author tonyl
  */
class RollCallResponseTest extends WordSpec with Matchers {

  "createForResponse" should {
    "create same unique token for same username, case-insensitive" in {
      val r1 = RollCallResponse.createForResponse("User 1", In, "will come", 1)
      val r2 = RollCallResponse.createForResponse("user 1", In, "will come", 1)

      r1.uniqueToken shouldEqual r2.uniqueToken
    }
  }
}
