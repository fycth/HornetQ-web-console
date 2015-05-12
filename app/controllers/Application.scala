package controllers

import akka.actor.{Props, ActorSystem}
import global.{Start, BusActor}
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object Application extends Controller {

  def index = Action {
    Ok(views.html.index(global.Global.listQueues(),global.Global.listTopics(),global.Global.getBusDetails()))
  }

  def topic(Name: String) = Action {
//    val name: String = UriEncoding.decodePathSegment(Name,SC.US_ASCII.name)
    val l = global.Global.listAllSubscriptions(Name)
    val c = global.Global.countTopicMessages(Name)
    Ok(views.html.topic(Name,l,c))
  }

  def queue(Name: String) = Action {
//    val name: String = UriEncoding.decodePathSegment(Name,SC.US_ASCII.name)
    val l = global.Global.listQueueConsumers(Name)
    val c = global.Global.countQueueMessages(Name)
    Ok(views.html.queue(Name,l,c))
  }

  def publishers() = Action {
    Ok(views.html.publishers(global.Global.listAllPublishers()))
  }

  def consumers() = Action {
    Ok(views.html.consumers(global.Global.listAllConsumers()))
  }

  def ws = WebSocket.using[String] { request =>
    val system = ActorSystem("HelloSystem")
    val (out, channel) = Concurrent.broadcast[String]
    // default Actor constructor
    val helloActor = system.actorOf(Props[BusActor], name = "helloactor")
//    helloActor ! "hello"
//    helloActor ! "buenos dias"
    helloActor ! Start(channel)

    // Log events to the console
    val in = Iteratee.foreach[String] { msg =>
    //println("Disconnected")
      helloActor ! msg
    }
    // Send a single 'Hello!' message
//    val out = Enumerator("Hello!")
    (in, out)
  }
}