package main.scala.akka.client.actor

import scala.concurrent.duration.DurationInt
import akka.actor.Actor
import akka.actor.ActorSelection.toScala
import main.scala.common.AkkaRequest
import main.scala.common.Start
import main.scala.common.TweetToServer

//#This actor simulates a sudden peak load on server by having a large number of followers and tweeting.
class PeakActor(startTime: Int, interval: Int, serverAddress: String, selfPath: String, name: String) extends Actor {

  import context.dispatcher

  def receive = {
    case Start =>
      val spike = context.system.scheduler.schedule((startTime * 1000) milliseconds, (interval * 1000) milliseconds, self, TweetToServer)
    case TweetToServer =>
      val servicePath = serverAddress + "/TweetsServiceRouter"
      val server = context.actorSelection(servicePath)
      println("creating spike load.")
      val uuid = java.util.UUID.randomUUID().toString()
      server ! new AkkaRequest(uuid, selfPath + name, "PostUpdate", name, "", getRandomText)
    case _ =>
      println("Unknown message received in Peak actor")
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