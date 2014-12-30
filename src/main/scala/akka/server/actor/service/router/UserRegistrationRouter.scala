package main.scala.akka.server.actor.service.router

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.Terminated
import akka.routing.ActorRefRoutee
import akka.routing.RoundRobinRoutingLogic
import akka.routing.Router
import main.scala.akka.server.actor.service.impl.UserRegistrationService
import main.scala.common.RegisterUser
import main.scala.common.RegisterUsers
import main.scala.common.Tweet
import main.scala.common.UserProfile

//#Accepts User registration from multiple clients and delegates request to multiple service actors to register the users to server.
class UserRegistrationRouter(count: Int, loadMonitor: ActorRef, userProfilesMap: scala.collection.mutable.Map[String, UserProfile], tweetsMap: scala.collection.mutable.Map[String, Tweet]) extends Actor {

  var router = {
    val routees = Vector.fill(count) {
      val r = context.actorOf(Props(new UserRegistrationService(count, loadMonitor, userProfilesMap, tweetsMap)))
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case RegisterUser(requestUUID: String, userName: String, selfPath: String) =>
      router.route(RegisterUser(requestUUID, userName, selfPath), sender)
    case RegisterUsers(uuid: String, ip: String, clients: Int, clientFactoryPath: String, followers: Array[Int], sampleSize: Int, peakActorName: String, peakActorFollowersCount: Int) =>
      router.route(RegisterUsers(uuid, ip, clients, clientFactoryPath, followers, sampleSize, peakActorName, peakActorFollowersCount), sender)
    case Terminated(a) =>
      router = router.removeRoutee(a)
      val r = context.actorOf(Props(new UserRegistrationService(count, loadMonitor, userProfilesMap, tweetsMap)))
      context watch r
      router = router.addRoutee(r)
    case _ =>
      println("Unknown Message received in User registration router.")
  }
}