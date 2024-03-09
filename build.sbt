name := "kiosk"
organization := "org.ergoplatform"
version := "1.0.0"
//updateOptions := updateOptions.value.withLatestSnapshots(false)
scalaVersion := "2.13.12"
licenses := Seq("CC0" -> url("https://creativecommons.org/publicdomain/zero/1.0/legalcode"))
publishTo := sonatypePublishToBundle.value
scmInfo := Some(
  ScmInfo(
    url("https://github.com/ergoplatform/kiosk"),
    "scm:git@github.com:ergoplatform/kiosk.git"
  )
)

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.11.0",
  "org.ergoplatform" %% "ergo-appkit" % "5.0.4",
  "com.squareup.okhttp3" % "mockwebserver" % "4.12.0",
  "org.scalatest" %% "scalatest" % "3.2.18",
  "org.scalatestplus" %% "scalacheck-1-17" % "3.2.18.0" % Test,
  "org.scalacheck" %% "scalacheck" % "1.17.0" % Test,
  "org.mockito" % "mockito-core" % "5.11.0" % Test
)

resolvers ++= Seq(
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "SonaType" at "https://oss.sonatype.org/content/groups/public",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

lazy val root = (project in file("."))
  .settings(
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    assembly / assemblyMergeStrategy := {
      case PathList("reference.conf")    => MergeStrategy.concat
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x                             => MergeStrategy.first
    }
  )

/* PUBLISHING */
ThisBuild / publishMavenStyle := true
Test / publishArtifact := true

// PGP key for signing a release build published to sonatype
// signing is done by sbt-pgp plugin
// how to generate a key - https://central.sonatype.org/pages/working-with-pgp-signatures.html
pgpPublicRing := file("ci/pubring.asc")
pgpSecretRing := file("ci/secring.asc")
pgpPassphrase := sys.env.get("PGP_PASSPHRASE").map(_.toArray)
usePgpKeyHex("D78982639AD538EF361DEC6BF264D529385A0333")

credentials ++= (for {
  username <- Option(System.getenv().get("SONATYPE_USERNAME"))
  password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
} yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq

// prefix version with "-SNAPSHOT" for builds without a git tag
ThisBuild / dynverSonatypeSnapshots := true
// use "-" instead of default "+"
ThisBuild / dynverSeparator := "-"