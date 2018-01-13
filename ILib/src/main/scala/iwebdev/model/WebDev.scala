package iwebdev.model


/**
  * The Info object is used as a transport object for the wire. Keeping it simple makes it easy
  * to create a codec. The ReplaceInfo structure / ADT could probably be improved upon.
  */
object WebDev {

  // Keeping this structure really simple as it needs to be
  // used for serialization in quite different environments

  type InfoType = String
  val JS: InfoType = "JS"
  val CSS: InfoType = "CSS"
  val INIT: InfoType = "INIT"
  val SBT_INFO: InfoType = "SBT_INFO"

  case class Info(
    id: String,
    `type`: InfoType,
    hash: Int,
    outputPath: String,
    domElementIndex: Int,
    content: String
  )

  // TODO: go through this ADT and see if there are ways to simplify ..
  sealed trait ReplaceInfo extends Product with Serializable {
    def toInfo = this match {
      case j:Js =>
        Info(j.id, JS, j.hash, j.outputPath, -1, j.content)
      case css:Css =>
        Info(css.id, CSS, css.hash, css.outputPath, -1, css.content)
      case sbtInfo: SBTInfo =>
        Info(sbtInfo.id, SBT_INFO, sbtInfo.hash, sbtInfo.outputPath, -1, sbtInfo.content)
      case init: Init.type =>
        createInit
    }
  }

  case class Js(
    id: String,
    hash: Int,
    outputPath: String,
    content: String
  ) extends ReplaceInfo

  case class Css(
    id: String,
    hash: Int,
    outputPath: String,
    content: String
  ) extends ReplaceInfo

  case class SBTInfo(
    id: String,
    hash: Int,
    outputPath: String,
    content: String
  )

  case object Init extends ReplaceInfo

  def createInit = Info(
    "", INIT, -1, "", -1, ""
  )

  def createInfo(
    id: String,
    outputPath: String,
    content: String,
    infoType: InfoType
  ): Info =
    Info(
      id, infoType, content.hashCode, outputPath, -1, content
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

      case SBT_INFO => SBTInfo(
        info.id,
        info.hash,
        info.outputPath,
        info.content
      )

      case INIT =>
        Init
    }
  }

}

