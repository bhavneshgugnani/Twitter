package main.scala.spray.client.main

import scala.concurrent.Future
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import main.scala.common.Constants
import main.scala.common.Start
import main.scala.spray.client.actor.SprayClientActorFactory
import main.scala.spray.client.actor.SprayPeakActor
import spray.can.Http
import spray.http._
import spray.http.ContentTypes._
import spray.http.HttpHeaders._
import spray.http.HttpMethods._
import spray.httpx.RequestBuilding._
import akka.dispatch.ExecutionContexts
import akka.util.Timeout

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
    val sprayRequestTimeout: Timeout = constants.TIMEOUT
    val serverAddress: String = hostAddress + ":" + constants.ROUTING_SERVER_PORT_FOR_HTTP_MESSAGES //constants.SPRAY_SERVER_PORT_FOR_HTTP_MESSAGES

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
      port = """ + constants.SPRAY_CLIENT_PORT_FOR_AKKA_MESSAGES + """
    }
 }
}"""

    val configuration = ConfigFactory.parseString(configString)
    implicit val system = ActorSystem("SprayClient", ConfigFactory.load(configuration))

    //Peak Load arguments. Optional.
    var peakActor: ActorRef = null
    var peakActorName: String = ""
    var peakActorFollowersCount: Int = 0
    try {
      val startTime: Int = args(4).toInt
      val interval: Int = args(5).toInt
      peakActorFollowersCount = args(6).toInt
      //val selfPath = "akka.tcp://Project4aClient@" + localAddress + ":" + constants.AKKA_CLIENT_PORT + "/user/PeakActor"
      peakActorName = "PeakActor"
      peakActor = system.actorOf(Props(new SprayPeakActor(startTime, interval, serverAddress, "PeakActor@" + localAddress, constants.TIMEOUT)), peakActorName)
    } catch {
      case ex: java.lang.ArrayIndexOutOfBoundsException => //Optional arguments for peak load. 
    }

    //#This class instantiates the user actors on client side and starts them when registration on server side is complete.
    val clientActorFactory = system.actorOf(Props(new SprayClientActorFactory(clients, serverAddress, followers, sampleSize, numberOfTweetsPerDay, offset, localAddress, timeMultiplier, peakActor, sprayRequestTimeout)), "ClientActorFactory")

    import system.dispatcher
    implicit val timeout: Timeout = constants.TIMEOUT
    
    println("Registering Clients on server : " + serverAddress)
    //Start load on server after 1 second
    system.scheduler.scheduleOnce( 5000 milliseconds, clientActorFactory, Start)
    for {
      response <- IO(Http).ask(HttpRequest(method = POST, uri = Uri(s"http://$serverAddress/userregistration?ip=" + localAddress.split(":")(0) + "&clients=" + clients + "&samplesize=" + sampleSize + "&peakactorname=" + peakActorName + "&peakactorfollowerscount=" + peakActorFollowersCount), entity = HttpEntity(`application/json`, """{ "ip" : """" + localAddress.split(":")(0) + """" , "clients" : """" + clients + """" , "sampleSize" : """" + sampleSize + """" , "peakActorName" : """" + peakActorName + """" , "peakActorFollowersCount" : """" + peakActorFollowersCount + """"}"""))).mapTo[HttpResponse]
      _ <- IO(Http) ? Http.CloseAll
    } yield {
      //if (response.status.toString.equalsIgnoreCase("200"))
      //clientActorFactory ! Start
    }

    //val clientFactoryPath: String = "akka.tcp://Project4aClient@" + localAddress + ":" + constants.AKKA_CLIENT_PORT + "/user/ClientActorFactory"

    //val server = system.actorSelection(serverAddress + "/UserRegistrationRouter")
    //server ! RegisterUsers(localAddress, clients, clientFactoryPath, followers, sampleSize, peakActorName, peakActorFollowersCount)

    /*
    val sprayServerIP: String = args(0)
    val constants = new Constants() 
    val sprayServerPort: Int = constants.SPRAY_SERVER_PORT_FOR_HTTP_MESSAGES
    val host = sprayServerIP + ":" + sprayServerPort
    val system = ActorSystem()
    val actor = system.actorOf(Props(new SprayClientActor(host, sprayServerPort)), "SprayClient")
    actor ! TweetToServer
  */

  }
}