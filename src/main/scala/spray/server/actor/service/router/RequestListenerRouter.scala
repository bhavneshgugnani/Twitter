package main.scala.spray.server.actor.service.router

import scala.collection.mutable.Map

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.actor.actorRef2Scala
import akka.routing.ActorRefRoutee
import akka.routing.RoundRobinRoutingLogic
import akka.routing.Router
import main.scala.common._
import main.scala.spray.server.actor.service.impl.RequestListenerService
import spray.can.Http
import spray.http.HttpRequest

class RequestListenerRouter(count: Int, name: String, localAddress: String, localAkkaMessagePort: Int, akkaServerAddress: String, akkaServerPort: Int, followers: Array[Int], requestMap: Map[String, ActorRef]) extends Actor {

  var router = {
    val routees = Vector.fill(count) {
      val r = context.actorOf(Props(new RequestListenerService(name, localAddress, localAkkaMessagePort, akkaServerAddress, akkaServerPort, followers, requestMap)))
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case _: Http.Connected => sender ! Http.Register(self)

    case HttpRequest(method, uri, header, entity, protocol) =>
      println("In router")
      router.route(HttpRequest(method, uri, header, entity, protocol), sender)
    case Start(requestUUID: String) =>
      router.route(Start(requestUUID), sender)
    case PostUpdateResponse(requestUUID: String) =>
      router.route(PostUpdateResponse(requestUUID), sender)
    case LoadUserTimelineResp(requestUUID: String, tweets: Map[String, String]) =>
      router.route(LoadUserTimelineResp(requestUUID, tweets), sender)
    case LoadHomeTimelineResp(requestUUID: String, tweets: Map[String, String]) =>
      router.route(LoadHomeTimelineResp(requestUUID, tweets), sender)
    case Terminated(a) =>
      router = router.removeRoutee(a)
      val r = context.actorOf(Props(new RequestListenerService(name, localAddress, localAkkaMessagePort, akkaServerAddress, akkaServerPort, followers, requestMap)))
      context watch r
      router = router.addRoutee(r)
    case a =>
      println("Unknown Message in Spray router : " + a.toString)
  }
}