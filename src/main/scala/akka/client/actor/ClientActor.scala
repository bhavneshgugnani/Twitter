package main.scala.akka.client.actor

import scala.collection.mutable.Map
import scala.concurrent.duration.DurationDouble
import akka.actor.Actor
import akka.actor.ActorSelection.toScala
import main.scala.common._

//#This class simulates normal user of tweeter through an actor. 
class ClientActor(serverAddress: String, followers: Int, tweetsPerDay: Int, offset: Double, name: String, totalClients: Int, timeMultiplier: Double) extends Actor {

  val constants = new Constants()
  val localAddress: String = java.net.InetAddress.getLocalHost.getHostAddress()
  val server = context.actorSelection(serverAddress + "/UserRegistrationRouter")
  val selfPath = "akka.tcp://Project4aClient@" + localAddress + ":" + constants.AKKA_CLIENT_PORT + "/user/"

  import context.dispatcher

  def receive = {
    case Start =>
      val tweetTimeout = ((24 * 3600) / (tweetsPerDay * timeMultiplier))
      val tweet = context.system.scheduler.schedule((offset / tweetsPerDay) * 1000 milliseconds, tweetTimeout * 1000 milliseconds, self, TweetToServer)
      val homeTimelineTimeout = ((24 * 3600) / (4 * timeMultiplier))
      val homeTimeline = context.system.scheduler.schedule((offset / 4) * 1000 milliseconds, homeTimelineTimeout * 1000 milliseconds, self, LoadHomeTimelineReq)
      val userTimelineTimeout = ((24 * 3600) / (1 * timeMultiplier))
      val userTimeline = context.system.scheduler.schedule((offset / 1) * 1000 milliseconds, userTimelineTimeout * 1000 milliseconds, self, LoadUserTimelineReq)
    case TweetToServer =>
      val servicePath = serverAddress + "/TweetsServiceRouter"
      val server = context.actorSelection(servicePath)
      val uuid = java.util.UUID.randomUUID().toString()
      server ! new AkkaRequest(uuid, selfPath + name, "PostUpdate", name, "", getRandomText)
    case PostUpdateResponse(requestUUID: String) =>
      //Trash response message
    case LoadHomeTimelineReq =>
      val servicePath = serverAddress + "/TimelineServiceRouter"
      val server = context.actorSelection(servicePath)
      val uuid = java.util.UUID.randomUUID().toString()
      server ! new AkkaRequest(uuid, selfPath + name, "GetHomeTimeline", name, "", "")
    case LoadHomeTimelineResp(uuid: String, tweets: Map[String, String]) =>
    //Trash Received tweets from server 
    case LoadUserTimelineReq =>
      val servicePath = serverAddress + "/TimelineServiceRouter"
      val server = context.actorSelection(servicePath)
      val uuid = java.util.UUID.randomUUID().toString()
      server ! new AkkaRequest(uuid, selfPath + name, "GetUserTimeline", name, "", "")
    case LoadUserTimelineResp(uuid: String, tweets: Map[String, String]) =>
    //Trash Received tweets from server
    case _ =>
      println("Unknown Message received at akka client actor")
  }

  //Generate random String for tweet text
  def getRandomText(): String = {
    val random = new scala.util.Random
    val length: Int = random.nextInt(120) + 20 //min 20 chars, max 140 chars
    val sb = new StringBuilder
    for (i <- 1 to length) {
      sb.append(random.nextPrintableChar)
    }
    sb.toString
  }
}
