package main.scala.common

import scala.collection.mutable.ListBuffer

//#Stores information about a user on server side.
class UserProfile(name: String, follower: ListBuffer[String], home: ListBuffer[String], user: ListBuffer[String]) {

  val username: String = name
  val followers = follower
  val homeTimeline = home
  val userTimeline = user
}