package com.whosin

import com.whosin.RunConfig.{App, Migrate}
import enumeratum.{Enum, EnumEntry}
import scopt.OParser

import scala.collection.immutable.IndexedSeq

/**
  * @author tony
  */

sealed trait RunConfig extends EnumEntry

case object RunConfig extends Enum[RunConfig] {

  case object Migrate extends RunConfig

  case object App extends RunConfig

  val values: IndexedSeq[RunConfig] = findValues

}

object Cli {

  def parse(args: Array[String]): Option[RunConfig] = {
    val builder = OParser.builder[RunConfig]
    val parser = {
      import builder._
      OParser.sequence(
        programName("whosin"),
        head("WhosInBot", "0.9"),
        cmd("migrate")
          .action((_, _) => Migrate)
          .text("Apply database migrations")
          .optional()
      )
    }

    OParser.parse(parser, args, App)
  }
}
