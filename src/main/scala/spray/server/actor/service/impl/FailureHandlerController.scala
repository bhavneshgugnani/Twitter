package main.scala.spray.server.actor.service.impl

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Terminated
import akka.actor.Props
import spray.can.Http
import akka.io.IO
import scala.collection.mutable.Map
import akka.actor.ActorSystem
import main.scala.spray.server.actor.service.router.RequestListenerRouter

class FailureHandlerController(cores: Int, localAddress: String, sprayServerPort: Int, numberOfPorts: Int, localAkkaMessagePort: Int, akkaServerAddress: String, akkaServerPort: Int, followers: Array[Int], requestMap: Map[String, ActorRef], handlerBindingPort: Int)(implicit val system: ActorSystem) extends Actor {

  var handler: ActorRef = null
  for (i <- sprayServerPort to (sprayServerPort + numberOfPorts - 1)) {
    handler = context.system.actorOf(Props(new RequestListenerService(i.toString, localAddress, localAkkaMessagePort, akkaServerAddress, akkaServerPort, followers, requestMap)), name = i.toString)
    //handler = system.actorOf(Props(new RequestListenerRouter(2 * cores, i.toString, localAddress, localAkkaMessagePort, akkaServerAddress, akkaServerPort, followers, requestMap)), name = i.toString)
    context watch handler
    IO(Http) ! Http.Bind(handler, interface = localAddress, port = i)
  }
  def receive = {
    case Terminated(a) =>
      println("Failed : " + a.toString)
    //val handler: ActorRef = context.system.actorOf(Props(new RequestListenerService(name, localAddress, localAkkaMessagePort, akkaServerAddress, akkaServerPort, followers, requestMap)), name = name)
    //IO(Http) ! Http.Bind(handler, interface = localAddress, port = handlerBindingPort)
    case a =>
      println("Unknown message received at FailureHandler Controller on spray server : " + a.toString)
  }
}