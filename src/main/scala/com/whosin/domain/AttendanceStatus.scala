package com.whosin.domain

import enumeratum.{Enum, EnumEntry}

import scala.collection.immutable.IndexedSeq

/**
  * @author tonyl
  */

sealed abstract class AttendanceStatus(override val entryName: String) extends EnumEntry {
  override def toString: String = entryName
}

object AttendanceStatus extends Enum[AttendanceStatus] {

  case object In extends AttendanceStatus("IN")

  case object Out extends AttendanceStatus("OUT")

  case object Maybe extends AttendanceStatus("MAYBE")

  override val values: IndexedSeq[AttendanceStatus] = findValues

}
