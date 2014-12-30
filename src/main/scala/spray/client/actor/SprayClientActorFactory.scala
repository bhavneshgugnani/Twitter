package main.scala.spray.client.actor

import scala.collection.mutable.ListBuffer
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import main.scala.common.Start
import akka.util.Timeout
import akka.actor.ActorSystem

class SprayClientActorFactory(clients: Int, serverAddress: String, followers: Array[Int], sampleSize: Int, numberOfTweetsPerDay: Array[Int], offset: Double, localAddress: String, timeMultiplier: Double, peakActor: ActorRef, sprayRequestTimeout: Timeout)(implicit system: ActorSystem) extends Actor {

  val clientActors = ListBuffer[ActorRef]()
  for (i <- 0 to clients - 1) {
    clientActors += context.system.actorOf(Props(new SprayClientActor(serverAddress, followers((i % sampleSize)), numberOfTweetsPerDay((i % sampleSize)), i * offset, "Client" + i + "@" + localAddress, clients, timeMultiplier, sprayRequestTimeout)), "Client" + i + "@" + localAddress)
  }
  def receive = {
    case Start =>
      println("Registration of clients on server successful. Starting Load on server.")
      if (peakActor != null)
        peakActor ! Start
      clientActors.foreach(ref => ref ! Start)
    case _ =>
      println("Unknown message received at client actor factory")
  }
}