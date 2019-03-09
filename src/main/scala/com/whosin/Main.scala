package com.whosin

import com.whosin.RunConfig.{App, Migrate}
import com.whosin.db.Migration

/**
  * @author tonyl
  */

// $COVERAGE-OFF$

object Main extends scala.App {

  Cli.parse(args) match {
    case Some(Migrate) => runMigration()
    case Some(App) => runApp()
    case _ =>
  }

  private def runMigration(): Unit = {
    Migration.migrate()
  }

  private def runApp(): Unit = {
    val app = new App()
    Runtime.getRuntime.addShutdownHook(new Thread(() => app.stop()))
    app.run()
  }

}
