package iwebdev.server

/**
  * Running the Server
  */
object IServer extends App {
  Program.processInfoStream.run.unsafeRunSync()
}


