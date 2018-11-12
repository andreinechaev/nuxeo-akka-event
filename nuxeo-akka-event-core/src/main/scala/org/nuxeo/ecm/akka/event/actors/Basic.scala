package org.nuxeo.ecm.akka.event.actors

import akka.actor.Actor
import org.apache.juli.logging.LogFactory
import org.nuxeo.ecm.core.event.{Event, EventBundle, EventListener, PostCommitEventListener}

import scala.collection.mutable

object Complete

class Basic(listenerClass: String, enabled: Boolean=true, async: Boolean=false, accept: mutable.Set[String]) extends Actor {

  private val log = LogFactory.getLog(this.getClass)

  private lazy val listener = Class.forName(listenerClass).getConstructors()(0).newInstance()

  override def receive: Receive = {

    case e: Event =>
      log.info(s"Received ${e.getName}")
      if (enabled && accept.contains(e.getName)) {
        val l = listener.asInstanceOf[EventListener]
        l.handleEvent(e)
        complete()
      }
    case eb: EventBundle =>
      log.info(s"Received bundle ${eb.getName}")
      if (enabled && accept.contains(eb.getName)) {
        val l = listener.asInstanceOf[PostCommitEventListener]
        l.handleEvent(eb)
        complete()
      }
    case v: Any => log.error(s"Unexpected message at ${this.getClass}: $v")
  }

  def complete(): Unit = {
    if (async) {
      sender ! Complete
    }
  }
}
