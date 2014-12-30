package main.scala.akka.server.actor.service.impl

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map
import scala.concurrent.duration.DurationInt

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSelection.toScala
import akka.actor.actorRef2Scala
import main.scala.common.AkkaRequest
import main.scala.common.InformLoad
import main.scala.common.InformRequestCount
import main.scala.common.LoadHomeTimelineResp
import main.scala.common.LoadUserTimelineResp
import main.scala.common.RegisterTimelineLoad
import main.scala.common.RegisterTweetRequestCount
import main.scala.common.Tweet
import main.scala.common.UserProfile

//#This serves any timeline service request coming from any user.
class TimelineService(loadMonitor: ActorRef, userProfilesMap: Map[String, UserProfile], tweetsMap: Map[String, Tweet]) extends Actor {
  import context.dispatcher

  var load: Int = 0
  var requestCount: Int = 0
  val updateLoad = context.system.scheduler.schedule(0 milliseconds, 2000 milliseconds, self, InformLoad)
  val updateRequestCount = context.system.scheduler.schedule(0 milliseconds, 2000 milliseconds, self, InformRequestCount)

  val numberOfTweetsPerRequest = 20

  def receive = {
    case AkkaRequest(requestUUID: String, requestActorPath: String, endPoint: String, userName: String, tweetuuid: String, tweetText: String) =>
      if (endPoint equalsIgnoreCase ("GetMentionsTimeline"))
        getMentionsTimeline(requestActorPath, endPoint, userName, tweetuuid, tweetText)
      else if (endPoint equalsIgnoreCase ("GetHomeTimeline"))
        getHomeTimeline(requestUUID, requestActorPath, endPoint, userName, tweetuuid, tweetText)
      else if (endPoint equalsIgnoreCase ("GetUserTimeline"))
        getUserTimeline(requestUUID, requestActorPath, endPoint, userName, tweetuuid, tweetText)
    case InformLoad =>
      loadMonitor ! RegisterTimelineLoad(load)
      load = 0
    case InformRequestCount =>
      loadMonitor ! RegisterTweetRequestCount(requestCount)
      requestCount = 0
    case _ => println("Unknown end point called form Tweet Service");
  }

  def getMentionsTimeline(requestActorPath: String, endPoint: String, userName: String, tweetuuid: String, tweetText: String) = {

  }

  def getHomeTimeline(requestUUID: String, requestActorPath: String, endPoint: String, userName: String, tweetuuid: String, tweetText: String): Unit = {
    try {
      val userProfile: UserProfile = userProfilesMap.get(userName).get
      val homeTimeline: ListBuffer[String] = userProfile.homeTimeline
      val tweets = Map[String, String]()
      var i = 1
      for (id <- homeTimeline if i <= numberOfTweetsPerRequest) {
        tweets += id -> tweetsMap.get(id).get.text
        i += 1
      }
      load += tweets.size
      requestCount += 1
      //val client = context.actorSelection(requestActorPath)
      //client ! new LoadHomeTimelineResp(requestUUID, tweets)
    } catch {
      case e: java.util.NoSuchElementException => //Ignore for Unregistered User println("Username : " + userName)
    }
  }

  def getUserTimeline(requestUUID: String, requestActorPath: String, endPoint: String, userName: String, tweetuuid: String, tweetText: String): Unit = {
    try {
      val userProfile: UserProfile = userProfilesMap.get(userName).get
      val userTimeline: ListBuffer[String] = userProfile.userTimeline
      val tweets = Map[String, String]()
      var i = 1
      for (id <- userTimeline if i <= numberOfTweetsPerRequest) {
        tweets += id -> tweetsMap.get(id).get.text
        i += 1
      }
      load += tweets.size
      requestCount += 1
      //val client = context.actorSelection(requestActorPath)
      //client ! new LoadUserTimelineResp(requestUUID, tweets)
    } catch {
      case e: java.util.NoSuchElementException => //Ignore for Unregistered User println("Username : " + userName)
    }
  }
}