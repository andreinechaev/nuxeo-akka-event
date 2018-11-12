package org.nuxeo.ecm.akka.event.service

import org.nuxeo.ecm.core.event.EventServiceComponent
import org.nuxeo.runtime.model.{ComponentContext, ComponentInstance}

class AkkaEventServiceComponent extends EventServiceComponent {

  override def activate(ctx: ComponentContext): Unit = {
    service = new AkkaEventService
  }

  override def registerContribution(contribution: Any, extensionPoint: String, contributor: ComponentInstance): Unit = {
    super.registerContribution(contribution, extensionPoint, contributor)
  }

  override def getApplicationStartedOrder: Int = -500
}
