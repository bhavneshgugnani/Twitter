package main.scala.akka.client.actor

import scala.collection.mutable.ListBuffer
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import main.scala.common.Start
import scala.concurrent.duration.DurationDouble

//#This class creates client actors to simulate users on client side.
class ClientActorFactory(clients: Int, serverAddress: String, followers: Array[Int], sampleSize: Int, numberOfTweetsPerDay: Array[Int], offset: Double, localAddress: String, timeMultiplier: Double, peakActor: ActorRef) extends Actor {

  val clientActors = ListBuffer[ActorRef]()
  for (i <- 0 to clients - 1) {
    clientActors += context.system.actorOf(Props(new ClientActor(serverAddress, followers((i % sampleSize)), numberOfTweetsPerDay((i % sampleSize)), i * offset, "Client" + i + "@" + localAddress, clients, timeMultiplier)), "Client" + i + "@" + localAddress)
  }
  
  import context.dispatcher
  val tweet = context.system.scheduler.scheduleOnce(5000 milliseconds, self, Start("start"))

  def receive = {
      case Start(requestUUID : String) =>
      println("Registration of clients on server successful. Starting Load on server.")
      if (peakActor != null)
        peakActor ! Start
      clientActors.foreach(ref => ref ! Start)
    case _ =>
      println("Unknown message received at client actor factory")
  }
}
