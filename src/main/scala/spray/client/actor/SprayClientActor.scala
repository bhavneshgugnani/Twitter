package main.scala.spray.client.actor

import akka.actor.ActorSystem
import akka.actor.Actor
import main.scala.common._
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
import spray.http._
import HttpMethods._
import spray.http.HttpHeaders._
import spray.http.ContentTypes._
import scala.concurrent.duration.DurationDouble

class SprayClientActor(serverAddress: String, followers: Int, tweetsPerDay: Int, offset: Double, name: String, totalClients: Int, timeMultiplier: Double, sprayRequestTimeout: Timeout)(implicit system: ActorSystem) extends Actor {

  import context.dispatcher
  
  implicit val timeout: Timeout = sprayRequestTimeout

  def receive = {
    case Start =>
      val tweetTimeout = ((24 * 3600) / (tweetsPerDay * timeMultiplier))
      val tweet = context.system.scheduler.schedule((offset / tweetsPerDay) * 1000 milliseconds, tweetTimeout * 1000 milliseconds, self, TweetToServer)
      val homeTimelineTimeout = ((24 * 3600) / (4 * timeMultiplier))
      val homeTimeline = context.system.scheduler.schedule((offset / 4) * 1000 milliseconds, homeTimelineTimeout * 1000 milliseconds, self, LoadHomeTimelineReq)
      val userTimelineTimeout = ((24 * 3600) / (1 * timeMultiplier))
      val userTimeline = context.system.scheduler.schedule((offset / 1) * 1000 milliseconds, userTimelineTimeout * 1000 milliseconds, self, LoadUserTimelineReq)
    case TweetToServer =>
      //println("Tweet : " + serverAddress + "/tweet/update/"+ name + "?tweet=" + getRandomText)
      for {
        response <- IO(Http).ask(HttpRequest(method = POST, uri = Uri(s"http://$serverAddress/tweet/update/"+ name + "?tweet=" + getRandomText), entity = HttpEntity(`application/json`, """{ "text" : """" + getRandomText + """"}"""))).mapTo[HttpResponse]
        _ <- IO(Http) ? Http.CloseAll
      } yield {
        //Stach tweet update
      }
    case LoadHomeTimelineReq =>
      for {
        response <- IO(Http).ask(HttpRequest(method = GET, uri = Uri(s"http://$serverAddress/timeline/hometimeline/" + name))).mapTo[HttpResponse]
        _ <- IO(Http) ? Http.CloseAll
      } yield {
        //Stash home timeline
      }
    case LoadUserTimelineReq =>
      for {
        response <- IO(Http).ask(HttpRequest(method = GET, uri = Uri(s"http://$serverAddress/timeline/usertimeline/"+ name))).mapTo[HttpResponse]
        _ <- IO(Http) ? Http.CloseAll
      } yield {
        //Stash user timeline
      }
    case _ =>
      println("Unknown message received at spray client actor.")
  }

  //Generate random String for tweet text
  def getRandomText(): String = {
    val random = new scala.util.Random
    val length: Int = random.nextInt(20) + 20 //min 20 chars, max 40 chars
    val sb = new StringBuilder
    for (i <- 1 to length) {
      //ASCII value 65 to 90 are printable characters
      sb.append((random.nextInt(25) + 65).toChar)
    }
    sb.toString
  }
}