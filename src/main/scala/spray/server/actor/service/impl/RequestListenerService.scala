package main.scala.spray.server.actor.service.impl

import akka.actor.Actor
import spray.can.Http
import spray.http._
import HttpMethods._
import main.scala.common._
import spray.json._
import DefaultJsonProtocol._
import spray.http.HttpHeaders._
import spray.http.ContentTypes._
import akka.actor.ActorRef
import scala.collection.mutable.Map

class RequestListenerService(name: String, localAddress: String, localAkkaMessagePort: Int, akkaServerAddress: String, akkaServerPort: Int, followers: Array[Int], requestMap: Map[String, ActorRef]) extends Actor {

  val selfPath = "akka.tcp://SprayServer@" + localAddress + ":" + localAkkaMessagePort + "/user/" + name
  val akkaServerPath = "akka.tcp://AkkaServer@" + akkaServerAddress + ":" + akkaServerPort + "/user/"

  def receive = {
    case _: Http.Connected => sender ! Http.Register(self)

    //Sample request
    case HttpRequest(POST, Uri.Path(path), header, entity, protocol) if path startsWith "/ping" =>
      println("PING")
      val args: Array[String] = path.split("/")
      println(header)
      val payload = entity.asString
      println(payload)
      val jsonPayload = payload.asJson
      val map = jsonPayload.convertTo[scala.collection.immutable.Map[String, String]] //Either[String, List[String]]]]
      val value = map.get("text").get
      println(value)
      sender ! HttpResponse(entity = HttpEntity(`application/json`, """{ received : """ + value + """}"""))

    //USER REGISTRATION
    //Register single user to akka server
    case HttpRequest(POST, Uri.Path(path), header, entity, protocol) if path startsWith "/registeruser" =>
      val args: Array[String] = path.split("/")
      val userName = args(2)
      var done = false
      var uuid: String = ""
      while (!done) {
        uuid = java.util.UUID.randomUUID().toString()
        if (requestMap.get(uuid) == None) {
          requestMap += uuid -> sender
          done = true
        }
      }
      //send request to akka server
      val akkaServer = context.actorSelection(akkaServerPath + "UserRegistrationRouter")
      akkaServer ! RegisterUser(uuid, userName, selfPath)

    case Complete(requestUUID: String) =>
      requestMap.remove(requestUUID).get ! HttpResponse()

    //Register multiple users to akka server
    case HttpRequest(POST, Uri.Path(path), header, entity, protocol) if path startsWith "/userregistration" =>
      val payloadMap = entity.asString.asJson.convertTo[scala.collection.immutable.Map[String, String]]
      val ip = payloadMap.get("ip").get
      val clients = payloadMap.get("clients").get.toInt
      //val clientFactoryPath = payloadMap.get("clientFactoryPath").get
      val sampleSize = payloadMap.get("sampleSize").get.toInt
      val peakActorName = payloadMap.get("peakActorName").get
      val peakActorFollowersCount = payloadMap.get("peakActorFollowersCount").get.toInt
      var done = false
      var uuid: String = ""
      while (!done) {
        uuid = java.util.UUID.randomUUID().toString()
        if (requestMap.get(uuid) == None) {
          requestMap += uuid -> sender
          done = true
        }
      }
      //send request to akka server
      val akkaServer = context.actorSelection(akkaServerPath + "UserRegistrationRouter")
      akkaServer ! RegisterUsers(uuid, ip, clients, selfPath, followers, sampleSize, peakActorName, peakActorFollowersCount)

    case Start(requestUUID: String) =>
      requestMap.remove(requestUUID).get ! HttpResponse()

    //TWEET SERVICES
    //POST Update
    case HttpRequest(POST, Uri.Path(path), header, entity, protocol) if path startsWith "/tweet/update" =>
      println("Tweet")
      val args: Array[String] = path.split("/")
      val service = args(1)
      val endPoint = "POST" + args(2)
      val userName = args(3)
      val payloadMap = entity.asString.asJson.convertTo[scala.collection.immutable.Map[String, String]]
      val tweetText = payloadMap.get("text").get
      var done = false
      var uuid: String = ""
      while (!done) {
        uuid = java.util.UUID.randomUUID().toString()
        if (requestMap.get(uuid) == None) {
          requestMap += uuid -> sender
          done = true
        }
      }
      //send request to akka server
      val akkaServer = context.actorSelection(akkaServerPath + "TweetsServiceRouter")
      akkaServer ! new AkkaRequest(uuid, selfPath, endPoint, userName, "", tweetText)

    //POST Update Response from akka server
    case PostUpdateResponse(requestUUID: String) =>
      requestMap.remove(requestUUID).get ! HttpResponse()

    //TIMELINE SERVICES
    //GET Usertimeline Request to akka server
    case HttpRequest(GET, Uri.Path(path), header, entity, protocol) if path startsWith "/timeline/usertimeline" =>
      val args: Array[String] = path.split("/")
      val service = args(1)
      val endPoint = "GET" + args(2)
      val userName = args(3)
      var done = false
      var uuid: String = ""
      while (!done) {
        uuid = java.util.UUID.randomUUID().toString()
        if (requestMap.get(uuid) == None) {
          requestMap += uuid -> sender
          done = true
        }
      }
      val akkaRequest = new AkkaRequest(uuid, selfPath, endPoint, userName, "", "")
      val akkaServer = context.actorSelection(akkaServerPath + "TimelineServiceRouter")
      akkaServer ! akkaRequest

    //Response from akka server for Usertimeline
    case LoadUserTimelineResp(requestUUID: String, tweets: Map[String, String]) =>
      requestMap.remove(requestUUID).get ! HttpResponse(entity = HttpEntity(`application/json`, """{ received : """ + tweets + """}"""))

    //GET Hometimeline Request to akka server
    case HttpRequest(GET, Uri.Path(path), header, entity, protocol) if path startsWith "/timeline/hometimeline" =>
      val args: Array[String] = path.split("/")
      val service = args(1)
      val endPoint = "GET" + args(2)
      val userName = args(3)
      var done = false
      var uuid: String = ""
      while (!done) {
        uuid = java.util.UUID.randomUUID().toString()
        if (requestMap.get(uuid) == None) {
          requestMap += uuid -> sender
          done = true
        }
      }
      val akkaRequest = new AkkaRequest(uuid, selfPath, endPoint, userName, "", "")
      val akkaServer = context.actorSelection(akkaServerPath + "TimelineServiceRouter")
      akkaServer ! akkaRequest

    //Response from akka server for Hometimeline
    case LoadHomeTimelineResp(requestUUID: String, tweets: Map[String, String]) =>
      requestMap.remove(requestUUID).get ! HttpResponse(entity = HttpEntity(`application/json`, """{ received : """ + tweets + """}"""))

    /*
      //TIMELINE SERVICES
    //GET Usertimeline Request to akka server
    case HttpRequest(GET, Uri.Path(path), header, entity, protocol) if path startsWith "/timeline/usertimeline" =>
      val args: Array[String] = path.split("/")
      val service = args(1)
      val endPoint = "GET" + args(2)
      val userName = args(3)
      val akkaRequest = new AkkaRequest(selfPath, endPoint, userName, "", "")
      val akkaServer = context.actorSelection(akkaServerPath + "TimelineServiceRouter")
    //akkaServer ! akkaRequest

    //Response from akka server for Usertimeline
    case LoadUserTimelineResp(tweets: Map[String, String]) =>
      println("Result Length : " + tweets.size)

    //GET Hometimeline Request to akka server
    case HttpRequest(GET, Uri.Path(path), header, entity, protocol) if path startsWith "/timeline/hometimeline" =>
      val args: Array[String] = path.split("/")
      val service = args(1)
      val endPoint = "GET" + args(2)
      val userName = args(3)
      val akkaRequest = new AkkaRequest(selfPath, endPoint, userName, "", "")
      val akkaServer = context.actorSelection(akkaServerPath + "TimelineServiceRouter")
      akkaServer ! akkaRequest

    //Response from akka server for Hometimeline
    case LoadHomeTimelineResp(tweets: Map[String, String]) =>
      println("Result Length : " + tweets.size)

      

    //GET Retweets
    case HttpRequest(GET, Uri.Path(path), header, entity, protocol) if path startsWith "/tweet/retweets" =>
      val args: Array[String] = path.split("/")
      val service = args(1)
      val endPoint = "Get" + args(2)
      val userName = args(3)
      val akkaRequest = new AkkaRequest(selfPath, endPoint, userName, "", "")
      val akkaServer = context.actorSelection(akkaServerPath + "TweetServiceRouter")
      akkaServer ! akkaRequest

    //GET Show
    case HttpRequest(GET, Uri.Path(path), header, entity, protocol) if path startsWith "/tweet/show" =>
      val args: Array[String] = path.split("/")
      val service = args(1)
      val endPoint = "Get" + args(2)
      val userName = args(3)
      val akkaRequest = new AkkaRequest(selfPath, endPoint, userName, "", "")
      val akkaServer = context.actorSelection(akkaServerPath + "TweetServiceRouter")
      akkaServer ! akkaRequest

    //POST Retweet
    case HttpRequest(POST, Uri.Path(path), header, entity, protocol) if path startsWith "/tweet/retweet" =>
      val args: Array[String] = path.split("/")
      val service = args(1)
      val endPoint = "Get" + args(2)
      val userName = args(3)
      val akkaRequest = new AkkaRequest(selfPath, endPoint, userName, "", "")
      val akkaServer = context.actorSelection(akkaServerPath + "TweetServiceRouter")
      akkaServer ! akkaRequest

    //POST Update
    case HttpRequest(POST, Uri.Path(path), header, entity, protocol) if path startsWith "/tweet/update" =>
      val args: Array[String] = path.split("/")
      val service = args(1)
      val endPoint = "Get" + args(2)
      val userName = args(3)
      val payload = entity.asString
      println(payload)
      val akkaRequest = new AkkaRequest(selfPath, endPoint, userName, "", "")
      val akkaServer = context.actorSelection(akkaServerPath + "TweetServiceRouter")
    //akkaServer ! akkaRequest

    //POST Destroy
    case HttpRequest(POST, Uri.Path(path), header, entity, protocol) if path startsWith "/timeline/destroy" =>
      val args: Array[String] = path.split("/")
      val service = args(1)
      val endPoint = "Get" + args(2)
      val userName = args(3)
      val akkaRequest = new AkkaRequest(selfPath, endPoint, userName, "", "")
      val akkaServer = context.actorSelection(akkaServerPath + "TweetServiceRouter")
      akkaServer ! akkaRequest

    case HttpRequest(GET, Uri.Path(path), header, entity, protocol) =>
      val args: Array[String] = path.split("/")
      val service = args(1)
      val endPoint = GET + args(2)
      val userName = args(3)
      val tweetuuid = args(4)

      if (service equalsIgnoreCase ("tweet")) {
        val akkaRequest = new AkkaRequest(selfPath, endPoint, userName, "", "")
        val akkaServer = context.actorSelection(akkaServerPath + "TweetsServiceRouter")
        //akkaServer ! akkaRequest
      } else if (service equalsIgnoreCase ("timeline")) {
        val akkaRequest = new AkkaRequest(selfPath, endPoint, userName, "", "")
        val akkaServer = context.actorSelection(akkaServerPath + "TimelineServiceRouter")
        //akkaServer ! akkaRequest
      }
      sender ! HttpResponse()
    case HttpRequest(POST, Uri.Path(path), header, entity, protocol) =>
      println(path)

    case AkkaResponse =>
    //send httprequest
*/
    /*case HttpRequest(GET, Uri.Path(path), _, _, _) if path startsWith "/tweet/update"=>
      println("TWEET RECEIVED")
      println(path)
      sender ! HttpResponse(entity = "TWEET RECEIVED!")
    case HttpRequest(GET, Uri.Path(path), _, _, _) if path startsWith "/timeout" =>
      println()
    case _: HttpRequest => sender ! HttpResponse(status = 404, entity = "Unknown resource!")*/

    //    case HttpRequest(method, uri, header, entity, protocol) =>
    //      println(method + " , " + uri + " , " + header + " , " + entity + " , " + protocol)
    //      sender ! HttpResponse(entity = "received")
    //      RESPONSE : GET , http://localhost:9080/ping , List(Accept-Language: en-US, en;q=0.8, Accept-Encoding: gzip, deflate, sdch, User-Agent: Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.65 Safari/537.36, Accept: text/html, application/xhtml+xml, application/xml;q=0.9, image/webp, */*;q=0.8, Connection: keep-alive, Host: localhost:9080) , Empty , HTTP/1.1

  }
}