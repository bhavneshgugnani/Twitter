package main.scala.akka.client.main

import com.typesafe.config.ConfigFactory
import akka.actor.ActorRef
import akka.actor.ActorSelection.toScala
import akka.actor.ActorSystem
import akka.actor.Props
import main.scala.akka.client.actor.ClientActorFactory
import main.scala.akka.client.actor.PeakActor
import main.scala.common.Constants
import main.scala.common.RegisterUsers

//#Client Main class to put load on Tweeter server for the project
object Main {

  def main(args: Array[String]) {
    val hostAddress: String = args(0)

    //Cranking factors from input
    val timeMultiplier: Double = args(1).toDouble
    val userCountMultiplier: Double = args(2).toDouble
    val tweetsCountMultiplier: Double = args(3).toDouble

    //default values
    val followers = Array(8, 7, 7, 5, 5, 3, 3, 1, 1, 1)
    val numberOfTweetsPerDay = Array(900, 400, 300, 200, 200, 100, 100, 100, 100, 100)
    var clients: Int = 1000000 //2840000
    val sampleSize: Int = 10

    //Scale tweets
    for (i <- 0 to numberOfTweetsPerDay.length - 1)
      numberOfTweetsPerDay(i) = (numberOfTweetsPerDay(i) * tweetsCountMultiplier).toInt

    //Scale User count
    clients = (clients * userCountMultiplier).toInt

    val localAddress: String = java.net.InetAddress.getLocalHost.getHostAddress()
    val constants = new Constants()
    val serverAddress: String = "akka.tcp://AkkaServer@" + hostAddress + ":" + constants.AKKA_SERVER_PORT + "/user"

    //Scale time
    val offset = (24 * 3600) / (clients * timeMultiplier)

    val configString = """akka {
  actor {
    provider = "akka.remote.RemoteActorRefProvider"
  }
  remote {
    enabled-transports = ["akka.remote.netty.tcp"]
    netty.tcp {
      hostname = """ + localAddress + """
      port = """ + constants.AKKA_CLIENT_PORT + """
    }
 }
}"""

    val configuration = ConfigFactory.parseString(configString)
    val system = ActorSystem("Project4aClient", ConfigFactory.load(configuration))

    //Peak Load arguments. Optional.
    var peakActor: ActorRef = null
    var peakActorName: String = ""
    var peakActorFollowersCount: Int = 0
    try {
      val startTime: Int = args(4).toInt
      val interval: Int = args(5).toInt
      peakActorFollowersCount = args(6).toInt
      val selfPath = "akka.tcp://Project4aClient@" + localAddress + ":" + constants.AKKA_CLIENT_PORT + "/user/PeakActor"
      peakActor = system.actorOf(Props(new PeakActor(startTime, interval, serverAddress, selfPath, "PeakActor@" + localAddress)), "PeakActor")
      peakActorName = "PeakActor"
    } catch {
      case ex: java.lang.ArrayIndexOutOfBoundsException => //Optional arguments for peak load. 
    }

    //#This class instantiates the user actors on client side and starts them when registration on server side is complete.
    val clientActorFactory = system.actorOf(Props(new ClientActorFactory(clients, serverAddress, followers, sampleSize, numberOfTweetsPerDay, offset, localAddress, timeMultiplier, peakActor)), "ClientActorFactory")
    val clientFactoryPath: String = "akka.tcp://Project4aClient@" + localAddress + ":" + constants.AKKA_CLIENT_PORT + "/user/ClientActorFactory"

    val server = system.actorSelection(serverAddress + "/UserRegistrationRouter")
    server ! RegisterUsers(java.util.UUID.randomUUID().toString(), localAddress, clients, clientFactoryPath, followers, sampleSize, peakActorName, peakActorFollowersCount)

  }
}
