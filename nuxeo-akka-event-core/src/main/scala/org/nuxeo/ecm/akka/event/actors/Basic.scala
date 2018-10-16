package org.nuxeo.ecm.akka.event.actors

import akka.actor.Actor
import org.apache.juli.logging.LogFactory
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor
import org.nuxeo.ecm.core.event.{Event, EventBundle}

object Complete

class Basic(listener: EventListenerDescriptor) extends Actor {

  private val log = LogFactory.getLog(this.getClass)

  override def receive: Receive = {
    case e: Event =>
      log.error(s"Received ${e.getName}")
      if (listener.isEnabled && listener.acceptEvent(e.getName)) {
        listener.asEventListener().handleEvent(e)
        complete(listener)
      }
    case eb: EventBundle =>
      log.error(s"Received bundle ${eb.getName}")
      if (listener.isEnabled && listener.acceptBundle(eb)) {
        listener.asPostCommitListener().handleEvent(eb)
        complete(listener)
      }
    case v: Any => log.error(s"Unexpected message at ${this.getClass}: $v")
  }

  def complete(listener: EventListenerDescriptor): Unit = {
    if (!listener.getIsAsync) {
      sender ! Complete
    }
  }
}
