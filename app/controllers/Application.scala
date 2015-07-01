package controllers

import akka.actor.{Props, ActorSystem}
import global.{Start, BusActor}
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data._
import play.api.data.Forms._

object Application extends Controller {

  val loginForm = Form(
    tuple(
      "hostname" -> text,
      "port" -> text,
      "username" -> text,
      "password" -> text
    )
  )

  def authenticate = Action { implicit request =>
    val (hostname, port, username, password) = loginForm.bindFromRequest.get
    if (global.Global.makeConnection(hostname,port,username,password)) {
      Redirect(routes.Application.index).withSession("username" -> username)
    } else {
      Redirect(routes.Application.login)
    }
  }

  def index = Action { implicit request =>
    request.session.get("username").map { username =>
      Ok(views.html.index(global.Global.listQueues(), global.Global.listTopics(), global.Global.getBusDetails()))
    }.getOrElse {
      Redirect(routes.Application.login)
    }
  }

  def topic(Name: String) = Action {
    val l = global.Global.listAllSubscriptions(Name)
    val c = global.Global.countTopicMessages(Name)
    Ok(views.html.topic(Name,l,c))
  }

  def queue(Name: String) = Action {
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
    helloActor ! Start(channel)

    // Log events to the console
    val in = Iteratee.foreach[String] { msg =>
      helloActor ! msg
    }
    (in, out)
  }

  def login = Action {
    Ok(views.html.login())
  }

  def logout = Action {
    Redirect(routes.Application.login).withNewSession
  }
}