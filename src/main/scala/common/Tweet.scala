package main.scala.common

//#Stores information of a tweet on server side.
class Tweet(uid: String, tweettext: String, username: String) {

  val uuid: String = uid
  val text: String = tweettext
  val userName: String = username
}