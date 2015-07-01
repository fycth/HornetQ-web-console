package lib

import java.util

import org.hornetq.api.core.{TransportConfiguration}
import org.hornetq.api.core.client._
import org.hornetq.api.core.management.{ResourceNames, ManagementHelper}
import org.hornetq.api.jms.HornetQJMSClient
import org.hornetq.core.remoting.impl.netty.{TransportConstants, NettyConnectorFactory}
import org.hornetq.jms.client.{HornetQQueueConnectionFactory, HornetQConnectionFactory}
import javax.jms._
import play.Play
import play.libs.Json

/**
 * @author Andrii Sergiienko
 * Created by Andrey on 5/8/2015.
 */

class HornetQWrapper(bus_host: String, bus_port: String, bus_user: String, bus_pswd: String) {
  private val cp = new util.HashMap[String,Object]()
  cp.put(TransportConstants.PORT_PROP_NAME,bus_port)
  cp.put(TransportConstants.HOST_PROP_NAME,bus_host)

  // Management Core Connection
  private val tc = new TransportConfiguration(classOf[NettyConnectorFactory].getName(),cp)
  private val l: ServerLocator = HornetQClient.createServerLocatorWithoutHA(tc)
  private val sf: ClientSessionFactory = l.createSessionFactory()
  private val cs: ClientSession = sf.createSession(bus_user,bus_pswd,false,true,true,true,1)
  private val cr: ClientRequestor = new ClientRequestor(cs,"jms.queue.hornetq.management")
  cs.start()
  //

  // Client JMS Connection
  private val jmsConnectionFactory: HornetQConnectionFactory  = new HornetQConnectionFactory(false, tc);
  private val jmsConnection: Connection  = jmsConnectionFactory.createConnection(bus_user,bus_pswd)
  jmsConnection.start()
  //

  def getHostname() : String = {
    return bus_host
  }
  def listQueues() : Map[String,Integer] = {
    val m: ClientMessage = cs.createMessage(false)
    ManagementHelper.putAttribute(m, ResourceNames.JMS_SERVER, "queueNames")
    val reply: ClientMessage = cr.request(m)
    val queueNamesO: Array[Object] = ManagementHelper.getResult(reply).asInstanceOf[Array[Object]]
    var map: Map[String,Integer] = Map[String,Integer]()
    for (queueName <- queueNamesO) map += (queueName.toString() -> messagesCount(queueName.toString(),ResourceNames.JMS_QUEUE))
    return map
  }

  def listTopics() : Map[String,Integer] = {
    val m: ClientMessage = cs.createMessage(false)
    ManagementHelper.putAttribute(m, ResourceNames.JMS_SERVER, "topicNames")
    val reply: ClientMessage = cr.request(m)
    val topicNamesO: Array[Object] = ManagementHelper.getResult(reply).asInstanceOf[Array[Object]]
    var map: Map[String,Integer] = Map[String,Integer]()
    for (topicName <- topicNamesO) map += (topicName.toString() -> messagesCount(topicName.toString(),ResourceNames.JMS_TOPIC))
    return map
  }

  def messagesCount(Name: String, qtype: String) : Integer = {
    val m: ClientMessage = cs.createMessage(false)
    ManagementHelper.putAttribute(m, qtype + Name, "messageCount")
    val reply: ClientMessage = cr.request(m)
    return ManagementHelper.getResult(reply).asInstanceOf[Integer]
  }
  
  def listAllSubscriptions(Name: String) : Object = {
    val m: ClientMessage = cs.createMessage(false)
    ManagementHelper.putOperationInvocation(m,ResourceNames.JMS_TOPIC + Name,"listAllSubscriptionsAsJSON")
    val r: ClientMessage = cr.request(m)
    val o = ManagementHelper.getResult(r).asInstanceOf[String]
    val j = Json.parse(o)
    return j
  }

  def listAllPublishers() : Object = {
    val m: ClientMessage = cs.createMessage(false)
    ManagementHelper.putOperationInvocation(m,ResourceNames.CORE_SERVER,"listProducersInfoAsJSON")
    val r: ClientMessage = cr.request(m)
    val o = ManagementHelper.getResult(r).asInstanceOf[String]
    val j = Json.parse(o)
    return j
  }

  def listAllConsumers() : Object = {
    val m: ClientMessage = cs.createMessage(false)
    ManagementHelper.putOperationInvocation(m,ResourceNames.JMS_SERVER,"listAllConsumersAsJSON")
    val r: ClientMessage = cr.request(m)
    val o = ManagementHelper.getResult(r).asInstanceOf[String]
    val j = Json.parse(o)
    return j
  }

  def listQueueConsumers(name: String) : Object = {
    val m: ClientMessage = cs.createMessage(false)
    ManagementHelper.putOperationInvocation(m,ResourceNames.JMS_QUEUE + name,"listConsumersAsJSON")
    val r: ClientMessage = cr.request(m)
    val o = ManagementHelper.getResult(r).asInstanceOf[String]
    val j = Json.parse(o)
    return j
  }

  def getBusDetails() : Map[String,Object] = {
    var m: Map[String,Object] = Map[String,Object]()
    m += ("Version" -> getBusVersion())
    m += ("Clustered" -> getBusClustered())
    m += ("Connections" -> listConnections())
    return m
  }

  private def getBusVersion() : String = {
    val m: ClientMessage = cs.createMessage(false)
    ManagementHelper.putAttribute(m, ResourceNames.CORE_SERVER, "Version")
    val reply: ClientMessage = cr.request(m)
    return ManagementHelper.getResult(reply).asInstanceOf[String]
  }

  private def getBusClustered() : String = {
    val m: ClientMessage = cs.createMessage(false)
    ManagementHelper.putAttribute(m, ResourceNames.CORE_SERVER, "Clustered")
    val reply: ClientMessage = cr.request(m)
    val b: Boolean = ManagementHelper.getResult(reply).asInstanceOf[Boolean]
    if (b) return "Clustered" else return "Non-clustered"
  }

  private def listConnections() : Map[String,Integer] = {
    def adjust[A, B](m: Map[A, B], k: A)(f: B => B) = m.updated(k, f(m(k)))

    val m: ClientMessage = cs.createMessage(false)
    ManagementHelper.putOperationInvocation(m, ResourceNames.CORE_SERVER, "listRemoteAddresses")
    val r: ClientMessage = cr.request(m)
    val o = ManagementHelper.getResult(r).asInstanceOf[Array[Object]]
    var cons: Map[String,Integer] = Map[String,Integer]()
    for(c <- o) {
      val s: String = c.toString()
      val i: Integer = s indexOf ':'
      if (i > 0) {
        val st: String = s.substring(1, i)
        if (cons.contains(st)) cons = adjust(cons, st)(_ + 1) else cons += (st -> 1)
      }
    }
    return cons
  }

  def sendMessageToQueue(queueName: String, message: String, isTopic: Boolean) = {
    val jmsSession: Session  = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE)
    val mp: MessageProducer = jmsSession.createProducer({if (isTopic) jmsSession.createTopic(queueName) else jmsSession.createQueue(queueName)})
    val msg: TextMessage = jmsSession.createTextMessage(message)
    mp.send(msg)
    mp.close()
    jmsSession.close()
  }

  def consumeMessageFromQueue(queueName: String, isTopic: Boolean) : String = {
    val jmsSession: Session  = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    val mc: MessageConsumer = jmsSession.createConsumer({if (isTopic) jmsSession.createTopic(queueName) else jmsSession.createQueue(queueName)})
    val msg: Message = mc.receive(5000)
    mc.close()
    jmsSession.close()
    if (null == msg) return "no message"
    if (msg.isInstanceOf[TextMessage]) {
      return msg.asInstanceOf[TextMessage].getText
    } else return msg.toString
  }

  def removeMessages(Name: String, topic: Boolean) :String = {
    val res: String = { if (topic) ResourceNames.JMS_TOPIC else ResourceNames.JMS_QUEUE }
    val m: ClientMessage = cs.createMessage(false)
    ManagementHelper.putOperationInvocation(m,res + Name,"removeMessages",null)
    val r: ClientMessage = cr.request(m)
    ManagementHelper.getResult(r)
    return "Removed"
  }

  def closeConnection() = {
    cs.stop()
    jmsConnection.stop()
  }
}
