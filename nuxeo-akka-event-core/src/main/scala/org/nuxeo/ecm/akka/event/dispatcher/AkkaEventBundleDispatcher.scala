package org.nuxeo.ecm.akka.event.dispatcher

import java.util
import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, Props}
import org.nuxeo.ecm.akka.event.actors.{Basic, Complete}
import org.nuxeo.ecm.core.event.EventBundle
import org.nuxeo.ecm.core.event.pipe.EventPipeDescriptor
import org.nuxeo.ecm.core.event.pipe.dispatch.EventBundleDispatcher

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.pattern.ask
import akka.util.Timeout

class AkkaEventBundleDispatcher extends EventBundleDispatcher {

  protected val ess = ActorSystem("AkkaEventServiceSystem")

  protected val actors: mutable.Map[String, ActorRef] = mutable.Map[String, ActorRef]()

  override def init(list: util.List[EventPipeDescriptor], params: util.Map[String, String]): Unit = {
    val descriptors = list.asScala

    for (d <- descriptors) {
      actors.put(d.getName, ess.actorOf(Props[Basic], name = d.getName))
    }
  }

  override def sendEventBundle(eventBundle: EventBundle): Unit = {
    for ((_, ref) <- actors) {
      ref ! eventBundle
    }
  }

  override def waitForCompletion(l: Long): Boolean = {
    for ((_, ref) <- actors) {
      implicit val timeout: Timeout = Timeout(5, TimeUnit.SECONDS)
      ref ? Complete
    }

    true
  }

  override def shutdown(): Unit = {
    ess.terminate()
    Await.ready(ess.whenTerminated, Duration(1, TimeUnit.MINUTES))
  }
}
