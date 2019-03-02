package com.whosin.domain

/**
  * @author tonyl
  */

//noinspection TypeAnnotation
object RollCallStatus extends Enumeration {
  val Open = Value("OPEN")
  val Closed = Value("CLOSED")

  type RollCallStatus = Value
}
