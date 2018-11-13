package org.nuxeo.ecm.akka.event.pipe

import java.util

import org.nuxeo.ecm.core.event.EventBundle
import org.nuxeo.ecm.core.event.pipe.EventBundlePipe

class AkkaEventBundlePipe extends EventBundlePipe {

  override def initPipe(s: String, map: util.Map[String, String]): Unit = {}

  override def sendEventBundle(eventBundle: EventBundle): Unit = throw new NotImplementedError()

  override def waitForCompletion(l: Long): Boolean = false

  override def shutdown(): Unit = {}
}
