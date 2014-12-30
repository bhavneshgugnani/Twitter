package main.scala.test.spray.server


import scala.concurrent.duration._
import scala.concurrent.duration.DurationInt
import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.ActorSystem
import akka.pattern.ask
import main.scala.common.Start
import main.scala.common.TweetToServer
import spray.http._
import spray.http.ContentTypes._
import spray.http.HttpMethods._
import akka.io.IO
import spray.can.Http
import akka.util.Timeout

class Test(serverAddress: String, pingInterval: Int, timeoutPeriod: Timeout)(implicit system: ActorSystem) extends Actor {

  import system.dispatcher
  val interval: Int = pingInterval
  implicit val timeout: Timeout = timeoutPeriod
  var counter: Int = 0

  def receive = {
    case Start =>
      val tweet = context.system.scheduler.schedule(0 milliseconds, interval milliseconds, self, TweetToServer)
    case TweetToServer =>
      println("Ping : " + counter )
      counter += 1
      for {
        response <- IO(Http).ask(HttpRequest(method = POST, uri = Uri(s"http://$serverAddress/ping"), entity = HttpEntity(`application/json`, """{ "text" : "Hello!" }"""))).mapTo[HttpResponse]
        _ <- IO(Http) ? Http.CloseAll
      } yield {
        println("complete")
        //Stach tweet update
      }
  }
}