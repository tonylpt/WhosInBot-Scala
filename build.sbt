name := "WhosInBot-Scala"
version := "0.1"
scalaVersion := "2.12.8"

mainClass in assembly := Some("com.whosin.Main")

parallelExecution in Test := false

scalacOptions ++= Seq(
  "-encoding", "utf8",
  "-Xfatal-warnings",
  "-deprecation",
  "-unchecked",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps",
  "-feature",
)


lazy val akkaVersion = "2.5.21"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.scalamock" %% "scalamock" % "4.1.0" % Test,

  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.1.7",
  "com.typesafe" % "config" % "1.3.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "org.slf4j" % "jul-to-slf4j" % "1.7.25",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "io.sentry" % "sentry-logback" % "1.7.16",

  "com.bot4s" %% "telegram-core" % "4.0.0-RC2" excludeAll ExclusionRule(organization = "org.scalaj"),
  "com.bot4s" %% "telegram-akka" % "4.0.0-RC2" excludeAll ExclusionRule(organization = "com.typesafe.akka"),

  "org.postgresql" % "postgresql" % "42.2.5",
  "com.typesafe.slick" %% "slick" % "3.3.0",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.3.0",
  "com.github.tminglei" %% "slick-pg" % "0.17.2",
  "com.zaxxer" % "HikariCP" % "3.3.0",

  "com.roundeights" %% "hasher" % "1.2.0",
  "org.scala-lang.modules" %% "scala-async" % "0.9.7",
)
