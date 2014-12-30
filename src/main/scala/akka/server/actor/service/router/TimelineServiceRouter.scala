package main.scala.akka.server.actor.service.router

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.routing.ActorRefRoutee
import akka.routing.RoundRobinRoutingLogic
import akka.routing.Router
import main.scala.akka.server.actor.service.impl.TimelineService
import main.scala.common.AkkaRequest
import main.scala.common.Tweet
import main.scala.common.UserProfile

//#Receives Timeline request from users and routes the request to a service actor instance for processing request.
//#This also acts as the interface to the users on the server side for sending request to for processing.
class TimelineServiceRouter(count: Int, loadMonitor: ActorRef, userProfilesMap: scala.collection.mutable.Map[String, UserProfile], tweetsMap: scala.collection.mutable.Map[String, Tweet]) extends Actor {

  var load: Int = 0
  var router = {
    val routees = Vector.fill(count) {
      val r = context.actorOf(Props(new TimelineService(loadMonitor, userProfilesMap, tweetsMap)))
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }
  def receive = {
    case AkkaRequest(uuid: String, requestActorPath: String, endPoint: String, userName: String, tweetuuid: String, tweetText: String) =>
      router.route(AkkaRequest(uuid: String, requestActorPath: String, endPoint: String, userName: String, tweetuuid: String, tweetText: String), sender)
    case Terminated(a) =>
      router = router.removeRoutee(a)
      val r = context.actorOf(Props(new TimelineService(loadMonitor, userProfilesMap, tweetsMap)))
      context watch r
      router = router.addRoutee(r)
    case _ =>
      println("Unknown message received at Timeline service router.")
  }
}