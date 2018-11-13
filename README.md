## Nuxeo Akka Event

The package is an experimental work for replacing/extending Nuxeo Event Service with Akka

#### Motivation

Event System is a blood stream of any large system. A scalable and flexible implementation of it is a crucial milestone.
Nuxeo does not have a strict separation between Event-driven and Data-driven architecture. The first step towards scalability
was made by introducing Nuxeo-Stream. 

Despite the fact that Nuxeo-Stream build upon Kafka solves the problem of scalability at some extent it does not solve architectural
misconception while using as Nuxeo Event Service Dispatcher and Pipe.

Events still are not independent entities and still transformed into `ListenerWorker`s, therefore scheduled to run on `WorkManager`.
Introduction of Dispatchers and Pipes allows a relatively easy integration of other systems for delegation event processing but still does not
make the event system lightweight. 

#### Concept

In this POC, I want to introduce Akka as an alternative scalable approach for Nuxeo Event System. Akka is not a replacement or an alternative to Nuxeo Stream,
it is intended just and only to simplify Event System within the platform.

There are a few points to note and elaborate what event is and what is not:

- Asynchronous
- Not scheduled
- `Event` is a lightweight object containing only meta information
- might be dispatched across multiple systems, making the system Distributed.
- intended only for the internal processing within the system
- potentially restoreble

Nuxeo might be interested in a couple approaches that are allowed by an actor system. 

###### First:

Each Event Listener is acting as an actor. So, it exists in the Actor system manageable by Akka. It means that all actors are executed concurrently
so we are not allocating a `Thread` per listener and still allow them to be efficient. 

###### Second:

Each `Node` contains an actor or a several of them to receive events. It will require manual configuration on which events are getting where but still totally doable.

##### Third: (and one of the branches actually using it right now)

It is the simplest one. When Akka is used from Nuxeo Dispatcher and after leveraging the same pattern Nuxeo uses at the moment by scheduling `EventListener`s on
`WorkManager`. This approach may work since Dispatcher assumes custom parameters and we still can pursue a distributed system. 




