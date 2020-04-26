package net.croz.cargotracker.infrastructure.project.axon.logging

import com.google.common.collect.ImmutableList
import net.croz.cargotracker.infrastructure.project.axon.logging.stub.aggregate.MyTestAggregate
import net.croz.cargotracker.infrastructure.project.axon.logging.stub.command.CreateMyTestAggregateCommand
import net.croz.cargotracker.infrastructure.project.axon.logging.stub.command.UpdateMyTestAggregateCommand
import net.croz.cargotracker.infrastructure.project.axon.logging.stub.projection.MyTestProjector
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.config.Configuration
import org.axonframework.config.Configurer
import org.axonframework.config.DefaultConfigurer
import org.axonframework.config.EventProcessingModule
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine
import org.axonframework.messaging.annotation.ClasspathHandlerDefinition
import org.axonframework.messaging.annotation.ClasspathHandlerEnhancerDefinition
import org.axonframework.messaging.annotation.MultiHandlerDefinition
import org.axonframework.messaging.annotation.MultiHandlerEnhancerDefinition
import spock.lang.Specification
import spock.util.concurrent.PollingConditions
import uk.org.lidalia.slf4jext.Level
import uk.org.lidalia.slf4jtest.LoggingEvent
import uk.org.lidalia.slf4jtest.TestLogger
import uk.org.lidalia.slf4jtest.TestLoggerFactory

class LoggingEventHandlerEnhancerDefinitionSpecification extends Specification {
  Configuration axonConfiguration
  CommandGateway axonCommandGateway

  def setup() {
    TestLoggerFactory.clearAll()
//    TestLoggerFactory.getInstance().setPrintLevel(Level.DEBUG) // uncomment if you want to see logging output during the test

    EventProcessingModule eventProcessingModule = new EventProcessingModule()
    eventProcessingModule.registerEventHandler((Configuration axonConfiguration) -> new MyTestProjector())

    Configurer axonConfigurer = DefaultConfigurer.defaultConfiguration()
    axonConfigurer.configureEmbeddedEventStore((Configuration axonConfiguration) -> new InMemoryEventStorageEngine())
                  .configureAggregate(MyTestAggregate)
                  .registerModule(eventProcessingModule)
                  .registerHandlerDefinition((Configuration configuration, Class inspectedClass) -> {
                    MultiHandlerDefinition multiHandlerDefinition = MultiHandlerDefinition.ordered(
                        MultiHandlerEnhancerDefinition.ordered(ClasspathHandlerEnhancerDefinition.forClass(inspectedClass), new LoggingEventHandlerEnhancerDefinition()),
                        ClasspathHandlerDefinition.forClass(inspectedClass)
                    )

                    return multiHandlerDefinition
                  })

    axonConfiguration = axonConfigurer.buildConfiguration()
    axonConfiguration.start()

    axonCommandGateway = axonConfiguration.commandGateway()
  }

  @SuppressWarnings("unused")
  def cleanup() {
    TestLoggerFactory.clearAll()

    axonConfiguration.shutdown()
    axonCommandGateway = null
    axonConfiguration = null
  }

  def "should work for event handler"() {
    given:
    TestLogger logger = TestLoggerFactory.getTestLogger("cargotracker.axon.event-handler-logging")
    String aggregateIdentifier = UUID.randomUUID().toString()

    when:
    axonCommandGateway.sendAndWait(new CreateMyTestAggregateCommand(aggregateIdentifier: aggregateIdentifier, name: "bla"))
    axonCommandGateway.sendAndWait(new UpdateMyTestAggregateCommand(aggregateIdentifier: aggregateIdentifier, sequenceNumber: 0, name: "ble"))

    then:
    new PollingConditions(timeout: 5, initialDelay: 0.5, delay: 0.5).eventually {
      ImmutableList<LoggingEvent> loggingEvents = logger.getAllLoggingEvents()
      loggingEvents.size() == 2
      loggingEvents[0].level == Level.DEBUG
      loggingEvents[0].message.contains(aggregateIdentifier)

      // sample message: Executing EventSourcingHandler method [MyTestAggregate.onMyTestAggregateCreatedEvent(MyTestAggregateCreatedEvent)] with event [MyTestAggregateCreatedEvent(aggregateIdentifier: 4bbf8ddd-6310-424d-8085-882f9df64200, sequenceNumber: 0)]
      loggingEvents[0].message ==~
      /Executing EventHandler method \[MyTestProjector.handle\(MyTestAggregateCreatedEvent\)] with event \[eventGlobalIndex: n\/a, eventId: \p{Graph}{36}, MyTestAggregateCreatedEvent\(aggregateIdentifier: \p{Graph}{36}, sequenceNumber: 0\)]/

      loggingEvents[1].level == Level.DEBUG
      loggingEvents[1].message.contains(aggregateIdentifier)

      // sample message: Executing EventHandler method [MyTestProjector.handle(MyTestAggregateUpdatedEvent)] with event [eventId: bd88fa8e-d834-4c93-8cc2-d39dc619d009, MyTestAggregateUpdatedEvent(aggregateIdentifier: 000580e3-5682-46be-8b41-8aabbb39f7b5, sequenceNumber: 1)]
      loggingEvents[1].message ==~
      /Executing EventHandler method \[MyTestProjector.handle\(MyTestAggregateUpdatedEvent\)] with event \[eventGlobalIndex: 1, eventId: \p{Graph}{36}, MyTestAggregateUpdatedEvent\(aggregateIdentifier: \p{Graph}{36}, sequenceNumber: 1\)]/
    }
  }

  def "should not log for logger level higher than DEBUG"() {
    given:
    TestLogger logger = TestLoggerFactory.getTestLogger("cargotracker.axon.event-handler-logging")
    logger.setEnabledLevelsForAllThreads(Level.INFO)
    String aggregateIdentifier = UUID.randomUUID().toString()

    when:
    axonCommandGateway.sendAndWait(new CreateMyTestAggregateCommand(aggregateIdentifier: aggregateIdentifier, name: "bla"))
    axonCommandGateway.sendAndWait(new UpdateMyTestAggregateCommand(aggregateIdentifier: aggregateIdentifier, sequenceNumber: 0, name: "ble"))

    then:
    new PollingConditions(timeout: 5, initialDelay: 0.5, delay: 0.5).eventually {
      ImmutableList<LoggingEvent> loggingEvents = logger.getAllLoggingEvents()
      loggingEvents.size() == 0
    }
  }
}