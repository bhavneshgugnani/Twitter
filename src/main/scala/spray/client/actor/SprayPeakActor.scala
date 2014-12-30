package main.scala.spray.client.actor

import akka.actor.Actor
import main.scala.common.TweetToServer
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import main.scala.common.AkkaRequest
import main.scala.common.Start
import akka.util.Timeout
import main.scala.common.TweetToServer
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import spray.http._
import spray.can.Http
import HttpMethods._
import spray.httpx.RequestBuilding._
import spray.http.HttpHeaders._
import spray.http.ContentTypes._

//#This actor simulates a sudden peak load on server by having a large number of followers and tweeting.
class SprayPeakActor(startTime: Int, interval: Int, serverAddress: String, name: String, sprayRequestTimeout: Timeout)(implicit system: ActorSystem) extends Actor {

  import context.dispatcher
  implicit val timeout: Timeout = sprayRequestTimeout

  def receive = {
    case Start =>
      val spike = context.system.scheduler.schedule((startTime * 1000) milliseconds, (interval * 1000) milliseconds, self, TweetToServer)
    case TweetToServer =>
      println("creating spike load.")
      val uuid = java.util.UUID.randomUUID().toString()
      for {
        //response <- IO(Http).ask(HttpRequest(method = POST, uri = Uri(s"http://$serverAddress/tweet/update/" + name), entity = """{ "text" : """" + getRandomText + """"}""", headers = List(`Content-Type`(`application/json`)))).mapTo[HttpResponse]
        response <- IO(Http).ask(HttpRequest(method = POST, uri = Uri(s"http://$serverAddress/tweet/update/" + name + "?tweet=" + getRandomText), entity = HttpEntity(`application/json`, """{ "text" : """" + getRandomText + """"}"""))).mapTo[HttpResponse]
        _ <- IO(Http) ? Http.CloseAll
      } yield {
        //println("Returned")
        //system.log.info("Request-Level API: received {} response with {} bytes", response.status, response.entity.data.length)
        response.header[HttpHeaders.Server].get.products.head
      }
    case _ =>
      println("Unknown message received in Spray Peak actor")
  }

  //Generate random String for tweet text
  def getRandomText(): String = {
    val random = new scala.util.Random
    val length: Int = random.nextInt(120) + 20 //min 20 chars, max 140 chars
    val sb = new StringBuilder
    for (i <- 1 to length) {
      //ASCII value 65 to 90 are printable characters
      sb.append((random.nextInt(25) + 65).toChar)
    }
    sb.toString
  }
}