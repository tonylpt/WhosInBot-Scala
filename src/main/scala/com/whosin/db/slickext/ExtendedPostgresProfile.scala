package com.whosin.db.slickext

import com.github.tminglei.slickpg.ExPostgresProfile
import slick.basic.Capability
import slick.jdbc.JdbcCapabilities

/**
  * This custom profile adds support for [[JdbcCapabilities.insertOrUpdate]] on PostgreSQL.
  *
  * @author tonyl
  */

class ExtendedPostgresProfile extends ExPostgresProfile {
  override def computeCapabilities: Set[Capability] = {
    super.computeCapabilities + JdbcCapabilities.insertOrUpdate
  }
}
