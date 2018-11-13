package org.nuxeo.ecm.akka.event.common

import java.util

import org.nuxeo.ecm.core.event.Event
import org.nuxeo.ecm.core.event.impl.ReconnectedEventBundleImpl

class AkkaReconnectedEventBundle extends ReconnectedEventBundleImpl {

  override def getReconnectedEvents: util.List[Event] = super.getReconnectedEvents
}
