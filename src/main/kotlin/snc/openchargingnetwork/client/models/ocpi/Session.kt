package snc.openchargingnetwork.client.models.ocpi

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Session(@JsonProperty("country_code") val countryCode: String,
                   @JsonProperty("party_id") val partyID: String,
                   @JsonProperty("id") val id: String,
                   @JsonProperty("start_date_time") val startDateTime: String,
                   @JsonProperty("stop_date_time") val stopDateTime: String? = null,
                   @JsonProperty("kwh") val kwh: Float,
                   @JsonProperty("cdr_token") val cdrToken: CdrToken,
                   @JsonProperty("auth_method") val authMethod: AuthMethod,
                   @JsonProperty("authorization_reference") val authorizationReference: String? = null,
                   @JsonProperty("location_id") val locationID: String,
                   @JsonProperty("evse_uid") val evseUID: String,
                   @JsonProperty("connector_id") val connectorID: String,
                   @JsonProperty("meter_id") val meterID: String? = null,
                   @JsonProperty("currency") val currency: String,
                   @JsonProperty("charging_periods") val chargingPeriods: List<ChargingPeriod>? = null,
                   @JsonProperty("total_cost") val totalCost: Price? = null,
                   @JsonProperty("status") val status: SessionStatus,
                   @JsonProperty("last_updated") val lastUpdated: String)

data class CdrToken(@JsonProperty("uid") val uid: String,
                    @JsonProperty("type") val tokenType: TokenType,
                    @JsonProperty("contract_id") val contractID: String)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ChargingPeriod(@JsonProperty("start_date_time") val startDateTime: String,
                          @JsonProperty("dimensions") val dimensions: List<CdrDimension>,
                          @JsonProperty("tariff_id") val tariffID: String? = null)

data class CdrDimension(@JsonProperty("type") val type: CdrDimensionType,
                        @JsonProperty("volume") val volume: Float)

data class Price(@JsonProperty("excl_vat") val excludingVat: Float,
                 @JsonProperty("incl_vat") val includingVat: Float)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ChargingPreferences(@JsonProperty("profile_type") val profileType: ProfileType,
                               @JsonProperty("departure_time") val departureTime: String? = null,
                               @JsonProperty("energy_need") val energyNeed: Float? = null,
                               @JsonProperty("discharge_allowed") val dischargeAllowed: Boolean? = null)

enum class CdrDimensionType {
    CURRENT,
    ENERGY,
    ENERGY_EXPORT,
    ENERGY_IMPORT,
    MAX_CURRENT,
    MIN_CURRENT,
    MAX_POWER,
    MIN_POWER,
    PARKING_TIME,
    POWER,
    RESERVATION_TIME,
    STATE_OF_CHARGE,
    TIME
}

enum class SessionStatus {
    ACTIVE,
    COMPLETED,
    INVALID,
    PENDING
}

enum class ProfileType {
    CHEAP,
    FAST,
    GREEN,
    REGULAR
}

enum class ChargingPreferencesResponse {
    ACCEPTED,
    DEPARTURE_REQUIRED,
    ENERGY_NEED_REQUIRED,
    NOT_POSSIBLE,
    PROFILE_TYPE_NOT_SUPPORTED
}