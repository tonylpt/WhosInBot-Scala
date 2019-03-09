package com.whosin.domain

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

/**
  * @author tonyl
  */

sealed abstract class RollCallStatus(override val entryName: String) extends EnumEntry {
  override def toString: String = entryName
}

object RollCallStatus extends Enum[RollCallStatus] {

  case object Open extends RollCallStatus("OPEN")

  case object Closed extends RollCallStatus("CLOSED")

  override val values: IndexedSeq[RollCallStatus] = findValues
}
