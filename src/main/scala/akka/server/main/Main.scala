package main.scala.akka.server.main

import java.util.concurrent.ConcurrentHashMap
import scala.collection.concurrent
import scala.collection.convert.decorateAsScala.mapAsScalaConcurrentMapConverter
import com.typesafe.config.ConfigFactory
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import main.scala.akka.server.actor.service.impl.LoadMonitor
import main.scala.akka.server.actor.service.router.TimelineServiceRouter
import main.scala.akka.server.actor.service.router.TweetsServiceRouter
import main.scala.akka.server.actor.service.router.UserRegistrationRouter
import main.scala.common.Constants
import main.scala.common.Tweet
import main.scala.common.UserProfile

//#This class is the main class to launch the tweeter server for this project. 
object Main {
  def main(args: Array[String]) {
    val constants = new Constants()

    val hostAddress: String = java.net.InetAddress.getLocalHost.getHostAddress()

    val configString = """akka {
  actor {
    provider = "akka.remote.RemoteActorRefProvider"
  }
  remote {
    enabled-transports = ["akka.remote.netty.tcp"]
    netty.tcp {
      hostname = """ + hostAddress + """
      port = """ + constants.AKKA_SERVER_PORT + """
    }
 }
}"""

    val configuration = ConfigFactory.parseString(configString)
    val cores: Int = Runtime.getRuntime().availableProcessors();
    val coresScaleUp: Int = 2 //Safe value for any core to be completely utilised

    val system = ActorSystem("AkkaServer", ConfigFactory.load(configuration))

    //#These maps store user profiles and tweets made to the tweeter server. They are concurrent maps and hence guarantee concurrent read and write operations to tweets and user profiles.
    val userProfilesMap: concurrent.Map[String, UserProfile] = new ConcurrentHashMap().asScala
    val tweetsMap: concurrent.Map[String, Tweet] = new ConcurrentHashMap().asScala

    createServiceRouter(system, cores, userProfilesMap, tweetsMap, constants)
  }

  def createServiceRouter(system: ActorSystem, cores: Int, userProfilesMap: scala.collection.mutable.Map[String, UserProfile], tweetsMap: scala.collection.mutable.Map[String, Tweet], constants: Constants) = {
    //Monitors Load on server
    val loadMonitor: ActorRef = createLoadMonitor(system, constants)

    //Registers User account on server for clients
    val userRegistrationRouter = system.actorOf(Props(new UserRegistrationRouter(cores * 2, loadMonitor, userProfilesMap, tweetsMap)), name = "UserRegistrationRouter")

    //Tweeter web-service routers
    val timelineServiceRouter = system.actorOf(Props(new TimelineServiceRouter(cores * 2, loadMonitor, userProfilesMap, tweetsMap)), name = "TimelineServiceRouter")
    val tweetsServiceRouter = system.actorOf(Props(new TweetsServiceRouter(cores * 2, loadMonitor, userProfilesMap, tweetsMap)), name = "TweetsServiceRouter")

  }

  def createLoadMonitor(system: ActorSystem, constants: Constants): ActorRef = {
    //#This actor is a load monitor for server side and register load by receiving messages form various service actors.
    val loadMonitor = system.actorOf(Props(new LoadMonitor(constants.UPDATE_TIMEOUT)), name = "LoadMonitor")
    loadMonitor
  }
}