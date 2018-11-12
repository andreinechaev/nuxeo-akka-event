package org.nuxeo.ecm.akka.event.actors

import java.util.concurrent.TimeUnit

import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor.{Actor, OneForOneStrategy, SupervisorStrategy}
import org.apache.juli.logging.LogFactory
import org.nuxeo.ecm.core.api.NuxeoException

import scala.collection.mutable
import scala.concurrent.duration

sealed class ListenerWrapper(listenerClass: String, enabled: Boolean=true, async: Boolean=false, accept: mutable.Set[String]) {

}

class EventSupervisor extends Actor {

  private lazy val log = LogFactory.getLog(classOf[EventSupervisor])

  override def supervisorStrategy: SupervisorStrategy = {
    OneForOneStrategy(maxNrOfRetries = 42, withinTimeRange = duration.FiniteDuration(16, TimeUnit.SECONDS)) {
      case _: NuxeoException => Restart
      case _: NullPointerException => Restart
      case _: Exception => Escalate
    }
  }

  override def receive: Receive = {
    case lw: ListenerWrapper =>

    case v: Any => log.info(s"Supervisor received $v")
  }

}
