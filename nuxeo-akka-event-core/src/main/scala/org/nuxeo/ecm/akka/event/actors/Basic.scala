package org.nuxeo.ecm.akka.event.actors

import java.util
import java.util.function.Supplier

import akka.actor.Actor
import org.apache.juli.logging.LogFactory
import org.nuxeo.ecm.akka.event.common.AkkaReconnectedEventBundle
import org.nuxeo.ecm.core.api.CoreSession
import org.nuxeo.ecm.core.event._
import org.nuxeo.ecm.core.event.impl.{AsyncEventExecutor, EventBundleImpl, ReconnectedEventBundleImpl}
import org.nuxeo.runtime.api.Framework
import org.nuxeo.runtime.transaction.TransactionHelper

import scala.collection.JavaConverters._
import scala.collection.mutable

object Complete

object Completed

class Basic extends Actor {

  private val log = LogFactory.getLog(this.getClass)

  private lazy val esa = Framework.getService(classOf[EventServiceAdmin])

  private lazy val asyncExecutor = new AsyncEventExecutor()

  override def receive: Receive = {
    case eb: EventBundle =>
      log.info(s"Received bundle ${eb.getName}")
//      spreadBundle(eb)
      asyncExecutor.run(esa.getListenerList.getEnabledAsyncPostCommitListenersDescriptors, eb)
    case Complete => sender ! Completed
    case v: Any => log.error(s"Unexpected message at ${this.getClass}: $v")
  }

  // TODO: come up with a way to avoid work manager scheduling.
  private def spreadBundle(eb: EventBundle): Unit = {
    var reconnected: EventBundle = null
    if (!eb.isInstanceOf[ReconnectedEventBundleImpl]) {
      reconnected = eb
    } else {
      val tmpBundle: EventBundle = eb
      TransactionHelper.runInTransaction(new Supplier[Object] {
        override def get(): AnyRef = {
          def foo(): Unit = {
            val connectedBundle: EventBundle = new EventBundleImpl
            val sessions: util.Map[String, CoreSession] = new util.HashMap[String, CoreSession]
            val events: mutable.Buffer[Event] = tmpBundle.asInstanceOf[AkkaReconnectedEventBundle].getReconnectedEvents.asScala
            for (event <- events) {
              connectedBundle.push(event)
              val session: CoreSession = event.getContext.getCoreSession
              if (!sessions.keySet.contains(session.getRepositoryName)) sessions.put(session.getRepositoryName, session)
            }
            sessions.values.forEach(_.close())
            reconnected = connectedBundle
          }

          foo()
          null
        }
      })
    }

    esa.getListenerList.getAsyncPostCommitListenersDescriptors.asScala
      .foreach(pcl => {
        val filtered = pcl.filterBundle(reconnected)
        val listener = pcl.asPostCommitListener()
        schedule(listener, filtered)
      })
  }

  private def schedule(listener: PostCommitEventListener, eb: EventBundle): Unit = {
    if (eb.isInstanceOf[ReconnectedEventBundle]) {
      listener.handleEvent(eb)
    } else {
      TransactionHelper.runInTransaction(new Supplier[Object] {
        override def get(): AnyRef = {
          listener.handleEvent(eb)
          null
        }
      })
    }
  }
}
