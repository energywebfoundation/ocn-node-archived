package snc.openchargingnetwork.client.services


import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpMethod
import snc.openchargingnetwork.client.config.Properties
import snc.openchargingnetwork.client.models.*
import snc.openchargingnetwork.client.models.entities.*
import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.repositories.*
import snc.openchargingnetwork.client.tools.generateUUIDv4Token
import snc.openchargingnetwork.contracts.RegistryFacade

class RoutingServiceTest {

    private val platformRepo: PlatformRepository = mockk()
    private val roleRepo: RoleRepository = mockk()
    private val endpointRepo: EndpointRepository = mockk()
    private val proxyResourceRepo: ProxyResourceRepository = mockk()
    private val httpRequestService: HttpRequestService = mockk()
    private val registry: RegistryFacade = mockk()
    private val credentialsService: CredentialsService = mockk()
    private val properties: Properties = mockk()

    private val routingService: RoutingService

    init {
        routingService = RoutingService(
                platformRepo,
                roleRepo,
                endpointRepo,
                proxyResourceRepo,
                registry,
                httpRequestService,
                credentialsService,
                properties)
    }

    @Test
    fun `prepareLocalPlatformRequest without proxy`() {
        val request = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                method = HttpMethod.GET,
                interfaceRole = InterfaceRole.RECEIVER,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = BasicRole("SNC", "DE"),
                receiver = BasicRole("ABC", "CH"),
                urlPathVariables = "DE/SNC/abc123",
                urlEncodedParameters = OcpiRequestParameters(type = TokenType.APP_USER),
                expectedResponseType = OcpiResponseDataType.TOKEN)

        every { routingService.getPlatformID(request.receiver) } returns 6L
        every { routingService.getPlatformEndpoint(
                platformID = 6L,
                module = request.module,
                interfaceRole = request.interfaceRole)
        } returns EndpointEntity(
                platformID = 6L,
                identifier = request.module.id,
                role = InterfaceRole.SENDER,
                url = "https://ocpi.cpo.com/2.2/tokens")

        every { platformRepo.findById(6L).get() } returns PlatformEntity(
                status = ConnectionStatus.CONNECTED,
                auth = Auth(tokenB = "1234567890", tokenC = generateUUIDv4Token()))

        val (url, headers) = routingService.prepareLocalPlatformRequest(request)

        assertThat(url).isEqualTo("https://ocpi.cpo.com/2.2/tokens/DE/SNC/abc123")
        assertThat(headers.authorization).isEqualTo("Token 1234567890")
        assertThat(headers.requestID.length).isEqualTo(36)
        assertThat(headers.correlationID).isEqualTo(request.correlationID)
        assertThat(headers.ocpiFromCountryCode).isEqualTo(request.sender.country)
        assertThat(headers.ocpiFromPartyID).isEqualTo(request.sender.id)
        assertThat(headers.ocpiToCountryCode).isEqualTo(request.receiver.country)
        assertThat(headers.ocpiToPartyID).isEqualTo(request.receiver.id)
    }


    @Test
    fun `prepareLocalPlatformRequest with proxy`() {
        val request = OcpiRequestVariables(
                module = ModuleID.CDRS,
                method = HttpMethod.GET,
                interfaceRole = InterfaceRole.SENDER,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = BasicRole("SNC", "DE"),
                receiver = BasicRole("ABC", "CH"),
                urlPathVariables = "67",
                expectedResponseType = OcpiResponseDataType.CDR_ARRAY)

        every { routingService.getPlatformID(request.receiver) } returns 126L
        every { routingService.getProxyResource(
                id = "67",
                sender = request.sender,
                receiver = request.receiver)
        } returns "https://cpo.com/cdrs?limit=20"

        every { platformRepo.findById(126L).get() } returns PlatformEntity(
                status = ConnectionStatus.CONNECTED,
                auth = Auth(tokenB = "0102030405", tokenC = generateUUIDv4Token()))

        val (url, headers) = routingService.prepareLocalPlatformRequest(request, proxied = true)

        assertThat(url).isEqualTo("https://cpo.com/cdrs?limit=20")
        assertThat(headers.authorization).isEqualTo("Token 0102030405")
        assertThat(headers.requestID.length).isEqualTo(36)
        assertThat(headers.correlationID).isEqualTo(request.correlationID)
        assertThat(headers.ocpiFromCountryCode).isEqualTo(request.sender.country)
        assertThat(headers.ocpiFromPartyID).isEqualTo(request.sender.id)
        assertThat(headers.ocpiToCountryCode).isEqualTo(request.receiver.country)
        assertThat(headers.ocpiToPartyID).isEqualTo(request.receiver.id)
    }


    @Test
    fun `prepareRemotePlatformRequest without proxy`() {
        val request = OcpiRequestVariables(
                module = ModuleID.TOKENS,
                method = HttpMethod.GET,
                interfaceRole = InterfaceRole.RECEIVER,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = BasicRole("SNC", "DE"),
                receiver = BasicRole("ABC", "CH"),
                urlPathVariables = "DE/SNC/abc123",
                urlEncodedParameters = OcpiRequestParameters(type = TokenType.APP_USER),
                expectedResponseType = OcpiResponseDataType.TOKEN_ARRAY)

        val sig = "0x9955af11969a2d2a7f860cb00e6a00cfa7c581f5df2dbe8ea16700b33f4b4b9" +
                "b69f945012f7ea7d3febf11eb1b78e1adc2d1c14c2cf48b25000938cc1860c83e01"

        val ocnBody = OcnMessageRequestBody(
                method = request.method,
                interfaceRole = request.interfaceRole,
                module = request.module,
                headers = OcpiRequestHeaders(
                        requestID = request.requestID,
                        correlationID = request.correlationID,
                        ocpiFromCountryCode = request.sender.country,
                        ocpiFromPartyID = request.sender.id,
                        ocpiToCountryCode = request.receiver.country,
                        ocpiToPartyID = request.receiver.id),
                urlPathVariables = request.urlPathVariables,
                urlEncodedParameters = request.urlEncodedParameters,
                body = request.body,
                expectedResponseType = request.expectedResponseType)

        every { registry.clientURLOf(
                request.receiver.country.toByteArray(),
                request.receiver.id.toByteArray()).sendAsync().get() } returns "https://ocn.client.net"

        val jsonString = jacksonObjectMapper().writeValueAsString(ocnBody)
        every { httpRequestService.mapper.writeValueAsString(ocnBody) } returns jsonString
        every { credentialsService.sign(jsonString) } returns sig

        val (url, headers, body) = routingService.prepareRemotePlatformRequest(request)

        assertThat(url).isEqualTo("https://ocn.client.net")
        assertThat(headers.requestID.length).isEqualTo(36)
        assertThat(headers.signature).isEqualTo(sig)
        assertThat(body).isEqualTo(ocnBody)
    }


    @Test
    fun `prepareRemotePlatformRequest with proxy`() {
        val request = OcpiRequestVariables(
                module = ModuleID.SESSIONS,
                method = HttpMethod.GET,
                interfaceRole = InterfaceRole.SENDER,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = BasicRole("SNC", "DE"),
                receiver = BasicRole("ABC", "CH"),
                urlPathVariables = "45",
                expectedResponseType = OcpiResponseDataType.SESSION_ARRAY)

        val sig = "0x9955af11969a2d2a7f860cb00e6a00cfa7c581f5df2dbe8ea16700b33f4b4b9" +
                "b69f945012f7ea7d3febf11eb1b78e1adc2d1c14c2cf48b25000938cc1860c83e01"

        val ocnBody = OcnMessageRequestBody(
                method = request.method,
                interfaceRole = request.interfaceRole,
                module = request.module,
                headers = OcpiRequestHeaders(
                        requestID = request.requestID,
                        correlationID = request.correlationID,
                        ocpiFromCountryCode = request.sender.country,
                        ocpiFromPartyID = request.sender.id,
                        ocpiToCountryCode = request.receiver.country,
                        ocpiToPartyID = request.receiver.id),
                urlPathVariables = request.urlPathVariables,
                urlEncodedParameters = request.urlEncodedParameters,
                body = request.body,
                proxyResource = "https://actual.cpo.com/ocpi/sender/2.2/sessions?limit=10&offset=50; rel =\"next\"",
                expectedResponseType = request.expectedResponseType)

        every { registry.clientURLOf(
                request.receiver.country.toByteArray(),
                request.receiver.id.toByteArray()).sendAsync().get() } returns "https://ocn-client.provider.net"

        every { routingService.getProxyResource("45", request.sender, request.receiver) } returns
                "https://actual.cpo.com/ocpi/sender/2.2/sessions?limit=10&offset=50; rel =\"next\""

        val jsonString = jacksonObjectMapper().writeValueAsString(ocnBody)
        every { httpRequestService.mapper.writeValueAsString(ocnBody) } returns jsonString
        every { credentialsService.sign(jsonString) } returns sig

        val (url, headers, body) = routingService.prepareRemotePlatformRequest(request, proxied = true)

        assertThat(url).isEqualTo("https://ocn-client.provider.net")
        assertThat(headers.requestID.length).isEqualTo(36)
        assertThat(headers.signature).isEqualTo(sig)
        assertThat(body).isEqualTo(ocnBody)
    }


    @Test
    fun proxyPaginationHeaders() {
        val request = OcpiRequestVariables(
                module = ModuleID.TARIFFS,
                method = HttpMethod.GET,
                interfaceRole = InterfaceRole.SENDER,
                requestID = generateUUIDv4Token(),
                correlationID = generateUUIDv4Token(),
                sender = BasicRole("SNC", "DE"),
                receiver = BasicRole("ABC", "CH"),
                urlEncodedParameters = OcpiRequestParameters(limit = 25),
                expectedResponseType = OcpiResponseDataType.TARIFF_ARRAY)

        val link = "https://some.link.com/ocpi/tariffs?limit=25&offset=25; rel=\"next\""

        val responseHeaders = mapOf(
                "Link" to link,
                "X-Limit" to "25",
                "X-Total-Count" to "148")

        every { proxyResourceRepo.save<ProxyResourceEntity>(any())
        } returns ProxyResourceEntity(
                resource = link,
                sender = request.sender,
                receiver = request.receiver,
                id = 74L)

        every { properties.url } returns "https://some.client.ocn"

        val proxyHeaders = routingService.proxyPaginationHeaders(request, responseHeaders)
        assertThat(proxyHeaders.getFirst("Link")).isEqualTo("https://some.client.ocn/ocpi/sender/2.2/tariffs/page/74; rel=\"next\"")
        assertThat(proxyHeaders.getFirst("X-Limit")).isEqualTo("25")
        assertThat(proxyHeaders.getFirst("X-Total-Count")).isEqualTo("148")
    }


    @Test
    fun getProxyResource() {
        val id = "123"
        val sender = BasicRole("SNC", "DE")
        val receiver = BasicRole("DIY", "UK")
        val resource = "https://some.co/ocpi/tokens?limit=10; rel=\"next\""
        every { proxyResourceRepo.findByIdOrNull(123L)?.resource } returns resource
        assertThat(routingService.getProxyResource(id, sender, receiver)).isEqualTo(resource)
    }


    @Test
    fun setProxyResource() {
        val resource = "https://some.co/ocpi/tokens?limit=10; rel=\"next\""
        val sender = BasicRole("SNC", "DE")
        val receiver = BasicRole("DIY", "UK")
        every { proxyResourceRepo.save(any<ProxyResourceEntity>()) } returns ProxyResourceEntity(
                resource = resource,
                sender = sender,
                receiver = receiver,
                id = 55L)
        assertThat(routingService.setProxyResource(resource, sender, receiver)).isEqualTo(55L)
    }


    @Test
    fun isRoleKnown() {
        val role = BasicRole("ABC", "FR")
        every { roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id) } returns true
        assertThat(routingService.isRoleKnown(role)).isEqualTo(true)
    }


    @Test
    fun isRoleKnownOnNetwork() {
        val role = BasicRole("XYZ", "US")
        every { registry.clientURLOf(role.country.toByteArray(), role.id.toByteArray()).sendAsync().get() } returns ""
        assertThat(routingService.isRoleKnownOnNetwork(role)).isEqualTo(false)
    }


    @Test
    fun getPlatformID() {
        val role = RoleEntity(5L, Role.CPO, BusinessDetails("SENDER Co"), "SEN", "DE")
        every { roleRepo.findByCountryCodeAndPartyIDAllIgnoreCase(role.countryCode, role.partyID) } returns role
        assertThat(routingService.getPlatformID(BasicRole(role.partyID, role.countryCode))).isEqualTo(5L)
    }


    @Test
    fun getPlatformEndpoint() {
        val endpoint = EndpointEntity(6L, "tokens", InterfaceRole.SENDER, "https://some.url.com")
        every { endpointRepo.findByPlatformIDAndIdentifierAndRole(
                platformID = endpoint.platformID,
                identifier = endpoint.identifier,
                Role = InterfaceRole.SENDER) } returns endpoint
        assertThat(routingService.getPlatformEndpoint(
                platformID = endpoint.platformID,
                module = ModuleID.TOKENS,
                interfaceRole = InterfaceRole.SENDER).url).isEqualTo(endpoint.url)
    }


    @Test
    fun getRemoteClientURL() {
        val role = BasicRole("XXX", "NL")
        every { registry.clientURLOf(role.country.toByteArray(), role.id.toByteArray()).sendAsync().get() } returns "https://some.client.com"
        assertThat(routingService.getRemoteClientUrl(role)).isEqualTo("https://some.client.com")
    }


    @Test
    fun `validateSender with auth only`() {
        val platform = PlatformEntity(id = 3L)
        every { platformRepo.findByAuth_TokenC("0102030405") } returns platform
        routingService.validateSender("Token 0102030405")
    }


    @Test
    fun `validateSender with auth and role`() {
        val role = BasicRole("YUT", "BE")
        val platform = PlatformEntity(id = 3L)
        every { platformRepo.findByAuth_TokenC("0102030405") } returns platform
        every { roleRepo.existsByPlatformIDAndCountryCodeAndPartyIDAllIgnoreCase(3L, role.country, role.id) } returns true
        routingService.validateSender("Token 0102030405", role)
    }


    @Test
    fun `validateReceiver should return LOCAL`() {
        val role = BasicRole("SNC", "DE")
        every { roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id) } returns true
        assertThat(routingService.validateReceiver(role)).isEqualTo(OcpiRequestType.LOCAL)
    }


    @Test
    fun `validateReceiver should return REMOTE`() {
        val role = BasicRole("SNC", "DE")
        every { roleRepo.existsByCountryCodeAndPartyIDAllIgnoreCase(role.country, role.id) } returns false
        every { registry.clientURLOf(role.country.toByteArray(), role.id.toByteArray()).sendAsync().get() } returns "http://localhost:8080"
        assertThat(routingService.validateReceiver(role)).isEqualTo(OcpiRequestType.REMOTE)
    }

}