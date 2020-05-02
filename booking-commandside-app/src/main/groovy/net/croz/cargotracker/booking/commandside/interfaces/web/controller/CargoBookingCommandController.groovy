package net.croz.cargotracker.booking.commandside.interfaces.web.controller

import groovy.transform.CompileStatic
import net.croz.cargotracker.booking.boundary.api.commandside.conversation.CargoBookResponse
import net.croz.cargotracker.booking.commandside.application.service.CargoBookingApplicationService
import net.croz.cargotracker.booking.commandside.interfaces.web.assembler.CargoBookingCommandAssembler
import net.croz.cargotracker.booking.commandside.interfaces.web.conversation.CargoBookWebRequest
import net.croz.cargotracker.infrastructure.project.boundary.api.conversation.OperationResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("/cargo-booking-command")
@CompileStatic
class CargoBookingCommandController {
  private final CargoBookingApplicationService cargoBookingApplicationService

  CargoBookingCommandController(CargoBookingApplicationService cargoBookingApplicationService) {
    this.cargoBookingApplicationService = cargoBookingApplicationService
  }

  @PostMapping("/cargo-book")
  OperationResponse<CargoBookResponse> cargoBook(@RequestBody CargoBookWebRequest cargoBookWebRequest, HttpServletRequest httpServletRequest) {
    OperationResponse<CargoBookResponse> cargoBookResponse = cargoBookingApplicationService.cargoBook(CargoBookingCommandAssembler.toCargoBookRequest(cargoBookWebRequest, httpServletRequest))
    return cargoBookResponse
  }
}
