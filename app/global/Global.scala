package global

/**
 * Created by Andrey on 5/8/2015.
 */

import java.text.SimpleDateFormat
import javax.jms.JMSSecurityException
import java.sql.Timestamp;

import lib.HornetQWrapper
import org.hornetq.api.core.management.ResourceNames
import play.Play
import play.api._
import play.api.libs.iteratee.Concurrent
import play.libs.Json
import akka.actor.Actor

case class Start(out: Concurrent.Channel[String])

class BusActor extends Actor {
//  var i: Integer = 0
  var out = {
    val (enum, chan) = Concurrent.broadcast[String]
    chan
  }

  def receive = {
    case Start(out) => this.out = out
    case msg: String       => {
      val j = Json.parse(msg)
      var msgtype: String = j.get("type").toString
      msgtype = msgtype.substring(1,msgtype.length-1)
      val qn: String = j.get("name").toString
      val t: String = j.get("qtype").toString
      if (msgtype.equals("sendmessage")) {
        val m: String = j.get("message").toString
        global.Global.sendMessageToQueue(qn.substring(1, qn.length - 1), m.substring(1, m.length - 1),t.toBoolean)
        this.out.push("Message sent")
      } else if (msgtype.equals("consumemessage")) {
        val msg: String = global.Global.consumeMessageFromQueue(qn.substring(1, qn.length - 1),t.toBoolean)
        this.out.push("Message received: " + msg)
      } else if (msgtype.equals("removemessages")) {
        val res = global.Global.removeMessages(qn.substring(1, qn.length - 1),t.toBoolean)
        this.out.push("Result: " + res)
      }
    }
  }
}

object Global extends GlobalSettings {
  var wrapper: HornetQWrapper = null
  def sendMessageToQueue(queueName: String, message: String, isTopic: Boolean) = {
    wrapper.sendMessageToQueue(queueName,message,isTopic)
  }

  def consumeMessageFromQueue(queueName: String, isTopic: Boolean) : String = {
    wrapper.consumeMessageFromQueue(queueName,isTopic)
  }

  def removeMessages(name: String, isTopic: Boolean) : String = {
    wrapper.removeMessages(name,isTopic)
  }

  def countQueueMessages(queueName: String) : Integer = {
    return wrapper.messagesCount(queueName,ResourceNames.JMS_QUEUE)
  }

  def countTopicMessages(queueName: String) : Integer = {
    return wrapper.messagesCount(queueName,ResourceNames.JMS_TOPIC)
  }

  def listQueues() : Map[String,Integer] = {
    return wrapper.listQueues()
  }

  def listTopics() : Map[String,Integer] = {
    return wrapper.listTopics()
  }

  def listAllSubscriptions(name: String) : Object = {
    return wrapper.listAllSubscriptions(name)
  }

  def listQueueConsumers(name: String) : Object = {
    return wrapper.listQueueConsumers(name)
  }

  def getMachineName() : String = {
    return Play.application().configuration().getString("bus.host")
  }

  def getBusDetails() : Map[String,Object] = {
    return wrapper.getBusDetails()
  }

  def listAllPublishers() : Object = {
    return wrapper.listAllPublishers()
  }

  def listAllConsumers() : Object = {
    return wrapper.listAllConsumers()
  }

  def timestampTotime(s: Long) : String = {
    val simpleDateFormat: SimpleDateFormat = new SimpleDateFormat("MM/dd/yyyy' 'HH:mm:ss");
    simpleDateFormat.format(new Timestamp(s))
  }

  override def onStart(app: Application) {
    Logger.info("Application has started")
    try {
      wrapper = new HornetQWrapper();
    } catch {
      case es: JMSSecurityException => Logger.error("JMS Security Exception when connecting to the bus: " + es);
      case e: Exception => Logger.error("Unknown exception happened when connecting to the bus: " + e);
    }
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
    wrapper.closeConnection()
  }

}