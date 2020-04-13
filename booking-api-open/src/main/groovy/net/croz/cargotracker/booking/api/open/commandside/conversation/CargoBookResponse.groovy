package net.croz.cargotracker.booking.api.open.commandside.conversation

import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import groovy.transform.PropertyOptions
import net.croz.cargotracker.lang.groovy.transform.options.RelaxedPropertyHandler

@PropertyOptions(propertyHandler = RelaxedPropertyHandler)
@MapConstructor
@CompileStatic
class CargoBookResponse {
  String aggregateIdentifier

  Map<String, ?> originLocation
  Map<String, ?> destinationLocation
}
