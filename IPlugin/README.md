# sbtLibby

Collection of often used libraries collected in a plugin for easy setup of projects

You have to build the project from source, jitpack does not support sbt plugin projects (if you know about similar service that does, please show me)

```bash
# clone the source repo, then
sbt publishLocal
```

Add it to project/plugins.sbt

```
addSbtPlugin("scalavision" % "sbtlibby" % "0.1-SNAPSHOT")
```

In your `build.sbt` file just add the common libraries you want to use:
```scala

enablePlugins(SbtLibby)

// Your spesific libraries
libraryDependencies ++= Seq(
 // Some library definitions
 // Then add the library composititions from this plugins
) ++ utilDeps.value ++ metaDeps.value // etc.
```

The library compositions are defined here:

https://github.com/scalavision/sbtLibby/blob/8c6b4f7548a4fa42f48ccc4cdc33b8d1cb50e23e/src/main/scala/sbtlibby/SbtLibby.scala#L149

Feel free to update or enhance the library compositions in PR's
