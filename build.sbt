import sbt.Keys._

lazy val commonSettings = Seq(
  scalaVersion := "2.12.4",
  version := "0.1-SNAPSHOT",
  organization := "scalavision",
  scalacOptions in Test ++= Seq("-Yrangepos"),
  libraryDependencies ++= Seq(
     "co.fs2" %% "fs2-core" % "0.10.0-M9",
     "co.fs2" %% "fs2-io" % "0.10.0-M9",
     "com.spinoco" %% "fs2-http" % "0.3.0-SNAPSHOT",
     "org.scodec" %% "scodec-bits" % "1.1.5",
     "org.scodec" %% "scodec-core" % "1.10.3",
     "com.github.benhutchison" %%% "prickle" % "1.1.13",
     "org.scodec" %% "scodec-stream" % "1.1.0-M9",
     "org.specs2" %% "specs2-core" % "4.0.0" % "test"
   )
)

lazy val IWebDevServer = project.in(file("IWebDevServer"))
  .settings(commonSettings :_*)

lazy val IWebDevPlugin = project.in(file("IWebDevPlugin"))
  .settings(commonSettings :_*)
  .settings(
    sbtPlugin := true,
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.21")
  )
  .dependsOn(IWebDevServer)

lazy val root = project.in(file("."))
  .aggregate(IWebDevServer, IWebDevPlugin)
