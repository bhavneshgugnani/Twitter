package main.scala.akka.server.actor.service.impl

import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map
import scala.concurrent.duration.DurationInt
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSelection.toScala
import akka.actor.Props
import akka.actor.Terminated
import akka.actor.actorRef2Scala
import akka.routing.ActorRefRoutee
import akka.routing.RoundRobinRoutingLogic
import akka.routing.Router
import main.scala.common._

//#This service registers users corresponding to a request from a client.
class UserRegistrationService(count: Int, loadMonitor: ActorRef, userProfilesMap: Map[String, UserProfile], tweetsMap: Map[String, Tweet]) extends Actor {
  import context.dispatcher

  var usersRegistered: Int = 0
  var jobID: Int = 0
  var jobMap = Map[Int, Job]()
  val useRegistered = context.system.scheduler.schedule(0 milliseconds, 2000 milliseconds, self, UpdateRegisteredUserCount)
  var userProfileCreatorRouter = {
    val routees = Vector.fill(count) {
      val r = context.actorOf(Props(new UserAccountCreatorActor()))
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  def receive = {
    case RegisterUser(requestUUID: String, userName: String, selfPath: String) =>
      val userProfile: UserProfile = new UserProfile(userName, new ListBuffer[String], new ListBuffer[String], new ListBuffer[String])
      userProfilesMap += userName -> userProfile
      usersRegistered += 1
      context .actorSelection(selfPath) ! Complete(requestUUID)
    case RegisterUsers(requestUUID: String, ip: String, clients: Int, clientFactoryPath: String, followers: Array[Int], sampleSize: Int, peakActorName: String, peakActorFollowersCount: Int) =>
      /*for (i <- 0 to clients - 1) {
        val userProfile: UserProfile = new UserProfile("Client" + i + "@" + ip, new ListBuffer[String], new ListBuffer[String], new ListBuffer[String])
        userProfilesMap += "Client" + i + "@" + ip -> userProfile
        val followerCount: Int = followers(i % sampleSize)
        val followerList: ListBuffer[String] = userProfile.followers
        for (k <- Math.max(0, i - followerCount) to i - 1) {
          followerList += "Client" + k + "@" + ip
        }
      }
      usersRegistered += clients
      //Register Peak user profile for spike
      if (peakActorName != "") {
        val userProfile: UserProfile = new UserProfile(peakActorName + "@" + ip, new ListBuffer[String], new ListBuffer[String], new ListBuffer[String])
        userProfilesMap += peakActorName + "@" + ip -> userProfile
        val followerList: ListBuffer[String] = userProfile.followers
        for (i <- 0 to Math.min(clients - 1, peakActorFollowersCount - 1))
          followerList += "Client" + i + "@" + ip
        usersRegistered += 1
      }
      val factory = context.actorSelection(clientFactoryPath)
      factory ! Start*/
      val taskCount = userProfileCreatorRouter.routees.length
      val taskSize: Int = Math.ceil(clients / taskCount).toInt
      jobMap += jobID -> new Job(jobID, requestUUID, taskCount, taskSize, clientFactoryPath)
      for (i <- 0 to taskCount - 1) {
        userProfileCreatorRouter.route(CreateUserProfiles(jobID, i * taskSize, Math.min(((i + 1) * taskSize) - 1, clients - 1), ip, userProfilesMap, followers, sampleSize, self.path.toString()), sender)
      }
      jobID += 1

      //Register Peak user profile for spike
      if (peakActorName != "") {
        val userProfile: UserProfile = new UserProfile(peakActorName + "@" + ip, new ListBuffer[String], new ListBuffer[String], new ListBuffer[String])
        userProfilesMap += peakActorName + "@" + ip -> userProfile
        val followerList: ListBuffer[String] = userProfile.followers
        for (i <- 0 to Math.min(clients - 1, peakActorFollowersCount - 1))
          followerList += "Client" + i + "@" + ip
        usersRegistered += 1
      }
    case TaskComplete(jobID) =>
      val job: Job = jobMap.get(jobID).get
      job.remainingJobs -= 1
      usersRegistered += job.jobSize
      if (job.remainingJobs == 0) {
        //send response message for registration complete
        //val factory = context.actorSelection(job.clientFactoryPath)
        //factory ! Start(job.requestUUID)
      }
    case UpdateRegisteredUserCount =>
      loadMonitor ! UserCount(usersRegistered)
      usersRegistered = 0
    case Terminated(a) =>
      userProfileCreatorRouter = userProfileCreatorRouter.removeRoutee(a)
      val r = context.actorOf(Props(new UserAccountCreatorActor()))
      context watch r
      userProfileCreatorRouter = userProfileCreatorRouter.addRoutee(r)
    case _ => println("Unknown message received in User Registration service.")
  }
}

class Job(jobID: Int, uuid: String, numberOfJobs: Int, jobsize: Int, clientfactorypath: String) {
  val requestUUID = uuid
  var remainingJobs: Int = numberOfJobs
  val clientFactoryPath = clientfactorypath
  val jobSize = jobsize
}