name := "fs2-das-example"

version := "0.1"

scalaVersion := "2.13.6"
addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)

libraryDependencies += "io.laserdisc" %% "fs2-aws-kinesis" % "4.0.0-RC3"

// https://mvnrepository.com/artifact/software.amazon.awssdk/kms
libraryDependencies += "software.amazon.awssdk" % "kms" % "2.16.44"
// https://mvnrepository.com/artifact/com.amazonaws/aws-encryption-sdk-java
libraryDependencies += "com.amazonaws" % "aws-encryption-sdk-java" % "2.3.3"
libraryDependencies += "org.typelevel" %% "kittens" % "2.3.2"

val circeVersion = "0.14.0"
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
  "io.circe" %% "circe-fs2",
  "io.circe" %% "circe-generic-extras",
).map(_ % circeVersion)
