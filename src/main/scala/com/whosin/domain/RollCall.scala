package com.whosin.domain

import java.util.Date

import com.whosin.domain.RollCallStatus.RollCallStatus
import com.whosin.domain.Util._

/**
  * @author tonyl
  */

case class RollCall(id: Option[Long],
                    chatId: Long,
                    status: RollCallStatus,
                    title: String,
                    quiet: Boolean,
                    createdAt: Date = now(),
                    updatedAt: Date = now())
