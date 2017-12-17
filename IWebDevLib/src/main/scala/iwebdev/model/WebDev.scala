package iwebdev.model

object WebDev {

  // Keeping this structure really simple as it needs to be
  // used for serialization in quite different environments

  val JS = "JS"
  val CSS = "CSS"

  case class WebDev(
    id: String,
    `type`: String,
    hash: Int,
    content: String
  )

}

