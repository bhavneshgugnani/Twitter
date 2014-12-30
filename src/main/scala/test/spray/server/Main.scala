package main.scala.test.spray.server

import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.util.Timeout
import main.scala.common.Constants
import main.scala.common.Start
import shapeless.ToInt

object Main {

  def main(args: Array[String]) {
    val constants = new Constants()
    val serverIP = args(0)
    val pingInterval = args(1)
    val serverAddress = serverIP + ":" + constants.SPRAY_SERVER_PORT_FOR_HTTP_MESSAGES

    implicit val system = ActorSystem("TestSprayServer")
    import system.dispatcher
    implicit val timeout: Timeout = constants.TIMEOUT
    println(serverAddress)
    
    val testActor = system.actorOf(Props(new Test(serverAddress, pingInterval.toInt, constants.TIMEOUT)), "TestActor")
    testActor ! Start
  }
}