package org.nuxeo.ecm.akka.event.service

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import akka.actor.Actor.Receive
import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{ActorRef, ActorSystem, OneForOneStrategy, Props, SupervisorStrategy}
import akka.pattern.ask
import akka.util.Timeout
import org.apache.juli.logging.LogFactory
import org.nuxeo.ecm.akka.event.actors._
import org.nuxeo.ecm.core.event._
import org.nuxeo.ecm.core.event.impl.{EventImpl, EventListenerDescriptor, EventServiceImpl}

import scala.collection.JavaConverters._
import scala.collection.{concurrent, mutable}
import scala.concurrent.{Await, duration}

class AkkaEventService extends EventServiceImpl with EventServiceAdmin {

  private val log = LogFactory.getLog(this.getClass)

  protected val ess = ActorSystem("EventServiceSystem")

  protected val listenerActorMap: concurrent.Map[String, ActorRef] = new ConcurrentHashMap[String, ActorRef].asScala

  protected val listenerMapping: concurrent.Map[String, mutable.Set[String]] = new ConcurrentHashMap[String, mutable.Set[String]].asScala

  def supervisorStrategy: SupervisorStrategy = {
    OneForOneStrategy(maxNrOfRetries = 42, withinTimeRange = duration.FiniteDuration(16, TimeUnit.SECONDS)) {
      case _: ArithmeticException => Resume
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: Exception => Escalate
    }
  }

  def receive: Receive = {
    case v: Any => log.warn(s"Supervisor got a message $v")
  }

  override def addEventListener(listener: EventListenerDescriptor): Unit = {
    val name = listener.getName
    log.error(s"Adding listener $name")
    val actor = ess.actorOf(Props(new Basic(listener)), name=name)
    listenerActorMap.put(name, actor)

    val events = listener.getEvents.asScala
    for (e <- events) {
      listenerMapping.get(e) match {
        case Some(s) =>
          s += name
          listenerMapping.put(e, s)
        case None =>
          val s = mutable.Set[String](name)

          listenerMapping.put(e, s)
      }
    }

    log.debug("Registered event listener: " + listener.getName)
  }

  override def removeEventListener(listener: EventListenerDescriptor): Unit = {
    listenerActorMap.remove(listener.getName)
    log.error("Unregistered event listener: " + listener.getName)
  }

  override def fireEvent(name: String, context: EventContext): Unit = {
    log.error(s"firing event $name")
    fireEvent(new EventImpl(name, context))
  }

  override def fireEvent(event: Event): Unit = {
    val name = event.getName
    fireEvent(name, event, event.isInline)
  }

  override def fireEventBundle(bundle: EventBundle): Unit = {
    val name = bundle.getName
    fireEvent(name, bundle, sync = false)
  }

  override def fireEventBundleSync(bundle: EventBundle): Unit = {
    val name = bundle.getName
    implicit val timeout: Timeout = Timeout(500, TimeUnit.MILLISECONDS)
    fireEvent(name, bundle)(timeout)
  }

  def fireEvent(name: String, event: Any, sync: Boolean = true)
               (implicit timeout: Timeout = Timeout(500, TimeUnit.MILLISECONDS)): Unit = {
    val listeners = listenerMapping.getOrElse(name, Set[String]())
    if (listeners.isEmpty) {
      log.warn(s"No listeners registered for $name")
      return
    }
    for (lname <- listeners) {
      listenerActorMap.get(lname) match {
        case Some(actor) =>
          if (sync) {
            val future = actor ? event
            Await.result(future, timeout.duration).asInstanceOf[Complete.type]
          } else {
            actor ! event
          }
        case None => log.warn(s"No listener with name $lname found")
      }

    }
  }

  override def waitForAsyncCompletion(timeout: Long): Unit = {
    super.waitForAsyncCompletion(timeout)
  }

  override def shutdown(timeout: Long): Unit = {
    super.shutdown(timeout)
    val _ = ess.terminate()

    for ((_, v) <- listenerMapping) {
      for (ln <- v) {
        listenerActorMap.get(ln) match {
          case Some(ref) => ess.stop(ref)
          case None => log.debug(s"Skipping $ln due to not able to find it")
        }
      }
    }

    implicit val t: Timeout = Timeout(timeout, TimeUnit.MILLISECONDS)
    Await.ready(ess.whenTerminated, t.duration)
  }

//  override def getEventListeners: util.List[EventListener] = ???
//
//  override def getPostCommitEventListeners: util.List[PostCommitEventListener] = ???
//
//  override def getEventListener(s: String): EventListenerDescriptor = ???
//
//  override def waitForAsyncCompletion(): Unit = ???
//
//  override def waitForAsyncCompletion(l: Long): Unit = ???
//
//  override def getEventsInQueueCount: Int = ???
//
//  override def getActiveThreadsCount: Int = ???
//
//  override def isBlockAsyncHandlers: Boolean = ???
//
//  override def setBlockAsyncHandlers(b: Boolean): Unit = ???
//
//  override def isBlockSyncPostCommitHandlers: Boolean = ???
//
//  override def setBlockSyncPostCommitHandlers(b: Boolean): Unit = ???
//
//  override def getListenerList: EventListenerList = ???
//
//  override def setListenerEnabledFlag(s: String, b: Boolean): Unit = ???
//
//  override def isBulkModeEnabled: Boolean = ???
//
//  override def setBulkModeEnabled(b: Boolean): Unit = ???
}
