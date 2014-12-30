package main.scala.spray.server.main

import java.util.concurrent.ConcurrentHashMap
import scala.collection.concurrent
import scala.collection.convert.decorateAsScala.mapAsScalaConcurrentMapConverter
import com.typesafe.config.ConfigFactory
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.io.IO
import akka.util.Timeout
import main.scala.common.Constants
import main.scala.spray.server.actor.service.router.RequestListenerRouter
import spray.can.Http
import main.scala.spray.server.actor.service.impl.RequestListenerService
import main.scala.spray.server.actor.service.impl.FailureHandlerController

object Main {

  def main(args: Array[String]) {
    val akkaServerIP = args(0)
    val localAddress: String = java.net.InetAddress.getLocalHost.getHostAddress()
    val constants = new Constants()
    
    val sprayServerPort: Int = constants.SPRAY_SERVER_PORT_FOR_HTTP_MESSAGES
    val numberOfPorts: Int = constants.NUMBER_OF_PORTS_FOR_SPRAY_SERVER
    
    val cores: Int = Runtime.getRuntime().availableProcessors();

    val requestMap: concurrent.Map[String, ActorRef] = new ConcurrentHashMap().asScala

    val configString = """akka {
  actor {
    provider = "akka.remote.RemoteActorRefProvider"
  }
  remote {
    enabled-transports = ["akka.remote.netty.tcp"]
    netty.tcp {
      hostname = """ + localAddress + """
      port = """ + constants.SPRAY_SERVER_PORT_FOR_AKKA_MESSAGES + """
    }
 }
}"""

    val configuration = ConfigFactory.parseString(configString)
    implicit val system = ActorSystem("SprayServer", ConfigFactory.load(configuration))
    implicit val timeout: Timeout = constants.TIMEOUT

    // the handler actor replies to incoming HttpRequests
    //var handler: ActorRef = null
    //handler = system.actorOf(Props(new RequestListenerService("RequestListener", localAddress, constants.SPRAY_SERVER_PORT_FOR_AKKA_MESSAGES, akkaServerIP, constants.AKKA_SERVER_PORT, constants.followers, requestMap)), name = "RequestListener")
    //handler = system.actorOf(Props(new RequestListenerRouter(2 * cores, "RequestListenerRouter", localAddress, constants.SPRAY_SERVER_PORT_FOR_AKKA_MESSAGES, akkaServerIP, constants.AKKA_SERVER_PORT, constants.followers, requestMap)), name = "RequestListenerRouter")
    //IO(Http) ! Http.Bind(handler, interface = localAddress, port = constants.SPRAY_SERVER_PORT_FOR_HTTP_MESSAGES)
    
    val controller: ActorRef = system.actorOf(Props(new FailureHandlerController(cores, localAddress, sprayServerPort, numberOfPorts, constants.SPRAY_SERVER_PORT_FOR_AKKA_MESSAGES, akkaServerIP, constants.AKKA_SERVER_PORT, constants.followers, requestMap, constants.SPRAY_SERVER_PORT_FOR_HTTP_MESSAGES)), "Controller")
  }
}
