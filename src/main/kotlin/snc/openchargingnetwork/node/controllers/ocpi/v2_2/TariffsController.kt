/*
    Copyright 2019 Share&Charge Foundation

    This file is part of Open Charging Network Node.

    Open Charging Network Node is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Open Charging Network Node is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Open Charging Network Node.  If not, see <https://www.gnu.org/licenses/>.
*/

package snc.openchargingnetwork.node.controllers.ocpi.v2_2

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import snc.openchargingnetwork.node.models.*
import snc.openchargingnetwork.node.models.ocpi.*
import snc.openchargingnetwork.node.services.HttpService
import snc.openchargingnetwork.node.services.RoutingService
import snc.openchargingnetwork.node.tools.isOcpiSuccess

@RestController
class TariffsController(private val routingService: RoutingService,
                        private val httpService: HttpService) {


    /**
     * SENDER INTERFACE
     */

    @GetMapping("/ocpi/sender/2.2/tariffs")
    fun getTariffsFromDataOwner(@RequestHeader("authorization") authorization: String,
                                @RequestHeader("X-Request-ID") requestID: String,
                                @RequestHeader("X-Correlation-ID") correlationID: String,
                                @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                @RequestParam("date_from", required = false) dateFrom: String?,
                                @RequestParam("date_to", required = false) dateTo: String?,
                                @RequestParam("offset", required = false) offset: Int?,
                                @RequestParam("limit", required = false) limit: Int?): ResponseEntity<OcpiResponse<Array<Tariff>>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlEncodedParams = OcpiRequestParameters(
                        dateFrom = dateFrom,
                        dateTo = dateTo,
                        offset = offset,
                        limit = limit))

        val response: HttpResponse<Array<Tariff>> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables)
            }

            Receiver.REMOTE -> {

                val (url, headers, body) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postOcnMessage(url, headers, body)
            }

        }

        val headers = routingService.proxyPaginationHeaders(
                request = requestVariables,
                responseHeaders = response.headers)

        return ResponseEntity
                .status(response.statusCode)
                .headers(headers)
                .body(response.body)
    }


    @GetMapping("/ocpi/sender/2.2/tariffs/page/{uid}")
    fun getTariffsPageFromDataOwner(@RequestHeader("authorization") authorization: String,
                                    @RequestHeader("X-Request-ID") requestID: String,
                                    @RequestHeader("X-Correlation-ID") correlationID: String,
                                    @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                    @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                    @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                    @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                    @PathVariable uid: String): ResponseEntity<OcpiResponse<Array<Tariff>>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
                interfaceRole = InterfaceRole.SENDER,
                method = HttpMethod.GET,
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = uid)

        val response: HttpResponse<Array<Tariff>> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables, proxied = true)

                httpService.makeOcpiRequest(url, headers, requestVariables)

            }

            Receiver.REMOTE -> {

                val (url, headers, body) = routingService.prepareRemotePlatformRequest(requestVariables, proxied = true)

                httpService.postOcnMessage(url, headers, body)

            }

        }

        var headers = HttpHeaders()

        if (isOcpiSuccess(response)) {

            routingService.deleteProxyResource(uid)

            headers = routingService.proxyPaginationHeaders(
                    request = requestVariables,
                    responseHeaders = response.headers)

        }

        return ResponseEntity
                .status(response.statusCode)
                .headers(headers)
                .body(response.body)
    }


    /**
     * RECEIVER INTERFACE
     */

    @GetMapping("/ocpi/receiver/2.2/tariffs/{countryCode}/{partyID}/{tariffID}")
    fun getClientOwnedTariff(@RequestHeader("authorization") authorization: String,
                             @RequestHeader("X-Request-ID") requestID: String,
                             @RequestHeader("X-Correlation-ID") correlationID: String,
                             @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                             @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                             @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                             @RequestHeader("OCPI-to-party-id") toPartyID: String,
                             @PathVariable countryCode: String,
                             @PathVariable partyID: String,
                             @PathVariable tariffID: String): ResponseEntity<OcpiResponse<Tariff>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.GET,
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/$countryCode/$partyID/$tariffID")

        val response: HttpResponse<Tariff> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables)

            }

            Receiver.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postOcnMessage(url, headers, ocnBody)

            }

        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }


    @PutMapping("/ocpi/receiver/2.2/tariffs/{countryCode}/{partyID}/{tariffID}")
    fun putClientOwnedTariff(@RequestHeader("authorization") authorization: String,
                             @RequestHeader("X-Request-ID") requestID: String,
                             @RequestHeader("X-Correlation-ID") correlationID: String,
                             @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                             @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                             @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                             @RequestHeader("OCPI-to-party-id") toPartyID: String,
                             @PathVariable countryCode: String,
                             @PathVariable partyID: String,
                             @PathVariable tariffID: String,
                             @RequestBody body: Tariff): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.PUT,
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/$countryCode/$partyID/$tariffID",
                body = body)

        val response: HttpResponse<Unit> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables)

            }

            Receiver.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postOcnMessage(url, headers, ocnBody)

            }

        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }


    @DeleteMapping("/ocpi/receiver/2.2/tariffs/{countryCode}/{partyID}/{tariffID}")
    fun deleteClientOwnedTariff(@RequestHeader("authorization") authorization: String,
                                @RequestHeader("X-Request-ID") requestID: String,
                                @RequestHeader("X-Correlation-ID") correlationID: String,
                                @RequestHeader("OCPI-from-country-code") fromCountryCode: String,
                                @RequestHeader("OCPI-from-party-id") fromPartyID: String,
                                @RequestHeader("OCPI-to-country-code") toCountryCode: String,
                                @RequestHeader("OCPI-to-party-id") toPartyID: String,
                                @PathVariable countryCode: String,
                                @PathVariable partyID: String,
                                @PathVariable tariffID: String): ResponseEntity<OcpiResponse<Unit>> {

        val sender = BasicRole(fromPartyID, fromCountryCode)
        val receiver = BasicRole(toPartyID, toCountryCode)

        routingService.validateSender(authorization, sender)

        val requestVariables = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
                interfaceRole = InterfaceRole.RECEIVER,
                method = HttpMethod.DELETE,
                headers = OcpiRequestHeaders(
                        requestID = requestID,
                        correlationID = correlationID,
                        sender = sender,
                        receiver = receiver),
                urlPathVariables = "/$countryCode/$partyID/$tariffID")

        val response: HttpResponse<Unit> = when (routingService.validateReceiver(receiver)) {

            Receiver.LOCAL -> {

                val (url, headers) = routingService.prepareLocalPlatformRequest(requestVariables)

                httpService.makeOcpiRequest(url, headers, requestVariables)

            }

            Receiver.REMOTE -> {

                val (url, headers, ocnBody) = routingService.prepareRemotePlatformRequest(requestVariables)

                httpService.postOcnMessage(url, headers, ocnBody)

            }

        }

        return ResponseEntity.status(response.statusCode).body(response.body)
    }

}