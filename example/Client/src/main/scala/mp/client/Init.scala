package mp.client
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

/**
  * Implements the `run` interface that IWebDev is calling.
  * Could probably make this more typesafe though ...
  */
@JSExportTopLevel("Init")
object Init {
  @JSExport
  def run(): Unit = {
    println("rerunning the app after compile ...")
    Application.run()
  }
}
