name := "knowledge-base-application-service"

version := "0.1"

scalaVersion := "2.12.8"

resolvers ++= Seq(
  "Twitter repository" at "http://maven.twttr.com"
)

lazy val finagleVersion = "19.11.0"
lazy val circeVersion = "0.12.3"
lazy val twitterServerVersion = "1.30.0"
lazy val finchVersion = "0.31.0"

libraryDependencies ++= Seq(
  "com.github.finagle" %% "finch-core" % finchVersion,
  "com.github.finagle" %% "finch-circe" % finchVersion,
  "com.twitter" %% "finagle-http" % finagleVersion,
  "com.twitter" %% "finagle-mysql" % finagleVersion,
  "com.twitter" %% "finagle-thrift" % finagleVersion,
  "com.twitter" %% "twitter-server" % twitterServerVersion,
  "com.twitter" %% "scrooge-core" % finagleVersion,
  "io.circe" %% "circe-generic" % circeVersion
)

com.twitter.scrooge.ScroogeSBT.newSettings