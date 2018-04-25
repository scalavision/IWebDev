import sbt.Keys._
import sbtcrossproject.{crossProject, CrossType}

lazy val scalaSetup = Seq(
  scalaVersion := "2.12.4",
  version := "0.1-SNAPSHOT",
  organization := "scalavision",
  scalacOptions in Test ++= Seq("-Yrangepos"),
)

lazy val commonSettings = Seq(
  libraryDependencies ++= Seq(
     "co.fs2" %% "fs2-core" % "0.10.2",
     "co.fs2" %% "fs2-io" % "0.10.2",
     "com.spinoco" %% "fs2-http" % "0.3.0",
     "org.scodec" %% "scodec-bits" % "1.1.5",
     "org.scodec" %% "scodec-core" % "1.10.3",
     "com.github.benhutchison" %% "prickle" % "1.1.14",
     "org.scodec" %% "scodec-stream" % "1.1.0",
     "org.specs2" %% "specs2-core" % "4.0.0" % "test",
     "com.lihaoyi" %% "pprint" % "0.5.3"
   )
)

lazy val LibSettings = Seq(
  scalaVersion := "2.12.4"
)

//lazy val ILib = project.in(file("ILib"))
//  .crossProject(JSPlatform, JVMPlatform)
lazy val ILib = (crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure) in file("ILib"))
  .settings(scalaSetup :_*)
  

lazy val ILibJS = ILib.js
lazy val ILibJVM = ILib.jvm

lazy val ICodec = (crossProject(JSPlatform, JVMPlatform).crossType(CrossType.Pure) in file("ICodec"))
  .settings(scalaSetup :_*)
  .jsSettings(
    libraryDependencies ++= Seq(
     "org.scodec" %%% "scodec-bits" % "1.1.5",
     "org.scodec" %%% "scodec-core" % "1.10.3",
     "com.github.benhutchison" %%% "prickle" % "1.1.14"
   ))
  .jvmSettings(
    libraryDependencies ++= Seq(
      "org.scodec" %% "scodec-bits" % "1.1.5",
      "org.scodec" %% "scodec-core" % "1.10.3",
      "com.github.benhutchison" %% "prickle" % "1.1.14"
    ))
  .dependsOn(ILib)



lazy val ICodecJS = ICodec.js
lazy val ICodecJVM = ICodec.jvm

lazy val IServer = project.in(file("IServer"))
  .settings(scalaSetup :_*)
  .settings(commonSettings :_*)
  .dependsOn(ILibJVM, ICodecJVM)


lazy val IClient = project.in(file("IClient"))
  .settings(scalaSetup :_*)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    //artifactPath in (Compile, fastOptJS) := file("/Users/tomsorlie/IdeaProjects/Projects/MindPointer/Client"),
    artifactPath in (Compile, fastOptJS) := 
      file(System.getProperty("user.home") + "/IdeaProjects/MindPointer/MindPointerClient/iclient-fastopt.js"),
    //  file(System.getProperty("user.home") + "/IdeaProjects/MindPointer/Client/iclient-fastopt.js"),
    libraryDependencies ++= Seq(
      "com.github.benhutchison" %%% "prickle" % "1.1.14",
      "org.scala-js" %%% "scalajs-dom" % "0.9.2",
      "org.scodec" %%% "scodec-bits" % "1.1.5",
      "com.lihaoyi" %%% "pprint" % "0.5.3"
    )
  )
  .dependsOn(ICodecJS)
  .enablePlugins(ScalaJSPlugin)

lazy val IPlugin = project.in(file("IPlugin"))
  .settings(scalaSetup :_*)
  .settings(commonSettings :_*)
  .settings(
    sbtPlugin := true,
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.21")
  )
  .dependsOn(IServer, ILibJVM, ICodecJVM)

lazy val root = project.in(file("."))
  .aggregate(IServer, IPlugin, ILibJVM, ILibJS, ICodecJVM, ICodecJS, IClient)
