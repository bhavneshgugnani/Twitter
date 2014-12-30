package main.scala.common

import akka.util.Timeout
import scala.concurrent.duration._

//#Contains various constants used in the project.  
class Constants {
  val AKKA_SERVER_PORT: Int = 7100
  val AKKA_CLIENT_PORT: Int = 7200
  val SPRAY_SERVER_PORT_FOR_HTTP_MESSAGES: Int = 7300
  val SPRAY_SERVER_PORT_FOR_AKKA_MESSAGES: Int = 7400
  val SPRAY_CLIENT_PORT_FOR_HTTP_MESSAGES: Int = 7500
  val SPRAY_CLIENT_PORT_FOR_AKKA_MESSAGES: Int = 7600
  val ROUTING_SERVER_PORT_FOR_HTTP_MESSAGES: Int = 7700
  val UPDATE_TIMEOUT: Int = 2000

  val followers = Array(8, 7, 7, 5, 5, 3, 3, 1, 1, 1)
  
  //Number of ports for spray
  val NUMBER_OF_PORTS_FOR_SPRAY_SERVER: Int = 10
  
  //Spray Timeout
  val TIMEOUT: Timeout = 30.seconds 
}
