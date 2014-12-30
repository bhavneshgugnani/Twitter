package main.scala.common

import scala.collection.mutable.Map

import akka.actor.ActorRef

//#All messsages sent in the project
sealed trait Messages

//#Request message to Akka server
case class AkkaRequest(requestUUID: String, requestActorPath: String, endPoint: String, userName: String, tweetuuid: String, tweetText: String) extends Messages
case class AkkaResponse(status: Boolean, akkaRequest: AkkaRequest) extends Messages

//#RequestListener messages
case class Timeline() extends Messages
case class Tweets() extends Messages
//#Assign service request to actor
case class ProcessService() extends Messages
//#Timeline messages
case class GetMentionsTimeline() extends Messages
case class ReturnMentionsTimeline(tweets: List[Tweet]) extends Messages
case class GetUserTimeline() extends Messages
case class ReturnUserTimeline(tweets: List[Tweet]) extends Messages
case class GetHomeTimeline() extends Messages
case class ReturnHomeTimeline(tweets: List[Tweet]) extends Messages
//#Tweets messages
case class GetRetweets() extends Messages
case class GetShow() extends Messages
case class GetOembed() extends Messages
case class PostRetweet() extends Messages
case class PostUpdate(tweet: String, favorites: Int) extends Messages
case class PostUpdateResponse(uuid: String) extends Messages
case class PostUpdateWithMedia() extends Messages
case class PostDestroy() extends Messages
//#
//case class Request(service: String, endPoint: String, tweet: String, followers: Int)

case class Print() extends Messages

//#Load Monitor Messages
case class MeasureLoad() extends Messages
case class InformLoad() extends Messages
case class InformRequestCount() extends Messages
case class RegisterTweetLoad(load: Int) extends Messages
case class RegisterTimelineLoad(load: Int) extends Messages
case class RegisterTweetRequestCount(count: Int) extends Messages
case class RegisterTimelineRequestCount(count: Int) extends Messages
case class PrintLoad() extends Messages
case class RegisterService(service: ActorRef) extends Messages
case class UserCount(count: Int) extends Messages

//#User Registration Messages
case class RegisterUser(requestUUID: String, userName: String, selfPath: String) extends Messages
case class RegisterUsers(requestUUID: String, ip: String, clients: Int, clientFactoryPath: String, followers: Array[Int], sampleSize: Int, peakActorName: String, peakActorFollowersCount: Int) extends Messages
case class RegistrationComplete(requestUUID: String) extends Messages
case class UpdateRegisteredUserCount() extends Messages
case class CreateUserProfiles(jobId: Int, start: Int, end: Int, ip: String, userProfilesMap: Map[String, UserProfile], followers: Array[Int], sampleSize: Int, senderPath: String) extends Messages
case class TaskComplete(jobID: Int) extends Messages

//#Client Messages
case class TweetToServer() extends Messages
case class LoadHomeTimeline() extends Messages
case class LoadHomeTimelineReq() extends Messages
case class LoadHomeTimelineResp(requestUUID: String, tweets: Map[String, String]) extends Messages
case class LoadUserTimelineReq() extends Messages
case class LoadUserTimelineResp(requestUUID: String, tweets: Map[String, String]) extends Messages

case class Complete(uuid: String) extends Messages
case class Start(uuid: String) extends Messages