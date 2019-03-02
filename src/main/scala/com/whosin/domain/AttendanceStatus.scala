package com.whosin.domain

/**
  * @author tonyl
  */

//noinspection TypeAnnotation
object AttendanceStatus extends Enumeration {
  val In = Value("IN")
  val Out = Value("OUT")
  val Maybe = Value("MAYBE")

  type AttendanceStatus = Value
}
