import com.typesafe.config.ConfigFactory

enablePlugins(FlywayPlugin)


flywayUrl := getDBUrl((resourceDirectory in Compile).value)
flywayUrl in Test := getDBUrl((resourceDirectory in Test).value)
flywayLocations += "db/migration"


def getDBUrl(resDir: File): String = {
  ConfigFactory.parseFile(resDir / "application.conf").getString("db.url")
}
