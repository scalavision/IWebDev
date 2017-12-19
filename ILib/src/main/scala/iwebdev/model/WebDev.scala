package iwebdev.model

object WebDev {

  // Keeping this structure really simple as it needs to be
  // used for serialization in quite different environments

  type InfoType = String
  val JS: InfoType = "JS"
  val CSS: InfoType = "CSS"

  case class Info(
    id: String,
    `type`: InfoType,
    hash: Int,
    outputPath: String,
    content: String
  )

  case class Js(
    id: String,
    hash: Int,
    outputPath: String,
    content: String
  )

  case class Css(
    id: String,
    hash: Int,
    outputPath: String,
    content: String
  )

  def createInfo(
    id: String,
    outputPath: String,
    content: String,
    infoType: InfoType
  ): Info =
    Info(
      id, infoType, content.hashCode, outputPath, content
    )

  def createJS(
    id: String,
    outputPath: String,
    content: String
  ) = Js(id, content.hashCode, outputPath, content)

  def createCss(
    id: String,
    outputPath: String,
    content: String
  ) = Css(id, content.hashCode, outputPath, content)



  def apply(info: Info) = {
    info.`type` match {
      case JS => Js(
        info.id,
        info.hash,
        info.outputPath,
        info.content
      )
      case CSS => Css(
        info.id,
        info.hash,
        info.outputPath,
        info.content
      )
    }
  }

}

