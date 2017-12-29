import sbt.Keys._
import sbtcrossproject.{crossProject, CrossType}

lazy val scalaSetup = Seq(
  scalaVersion := "2.12.4",
  version := "0.1-SNAPSHOT",
  organization := "scalavision",
  scalacOptions in Test ++= Seq("-Yrangepos"),
)

lazy val Client = project.in(file("."))
  .settings(scalaSetup :_*)
  .enablePlugins(ScalaJSPlugin, IWebDevPlugin)
  .settings(
 //   scalaJSUseMainModuleInitializer := true,
    outputJSPath in Compile := new java.io.File("out"),
    outputJSFilename in Compile := "mindpointer.js",
    //mainClass in Compile := Some("mp.client.MindPointer"),
    mainClass in Compile := Some("iwebdev.client.WebClient"),
    skip in packageJSDependencies := false,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.3",
      "scalavision" %%% "router4s" % "0.1-SNAPSHOT",
      "scalavision" %%% "rxlib"  % "0.1-SNAPSHOT"
//      "com.github.benhutchison" %%% "prickle" % "1.1.13"
//      "scalavision" %%% "iclient" % "0.1-SNAPSHOT"
    )
  )

