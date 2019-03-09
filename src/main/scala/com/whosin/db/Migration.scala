package com.whosin.db

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource

/**
  * @author tony
  */

object Migration extends StrictLogging {

  private val dataSource = {
    val ds = new PGSimpleDataSource()
    ds.setUrl(ConfigFactory.load().getString("db.url"))
    ds
  }

  val flyway: Flyway = Flyway.configure()
    .dataSource(dataSource)
    .locations("classpath:/db/migration")
    .load()

  def migrate(): Unit = {
    logger.info("Applying database migrations")
    flyway.migrate()
  }

}
