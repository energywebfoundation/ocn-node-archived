/*
    Copyright 2019 Share&Charge Foundation

    This file is part of Open Charging Network Client.

    Open Charging Network Client is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Open Charging Network Client is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Open Charging Network Client.  If not, see <https://www.gnu.org/licenses/>.
*/

package snc.openchargingnetwork.client.models.entities

import snc.openchargingnetwork.client.models.ocpi.*
import snc.openchargingnetwork.client.tools.generatePrivateKey
import snc.openchargingnetwork.client.tools.generateUUIDv4Token
import snc.openchargingnetwork.client.tools.getTimestamp
import javax.persistence.*


/**
 * Store the private key of the OCN client's wallet
 */
@Entity
@Table(name = "wallet")
class WalletEntity(var privateKey: String = generatePrivateKey(),
                   @Id val id: Long? = 1L)


/**
 * Stores an OCPI platform equivalent to a single OCPI connection to an OCN client
 */
@Entity
@Table(name = "platforms")
class PlatformEntity(var status: ConnectionStatus = ConnectionStatus.PLANNED,
                     var lastUpdated: String = getTimestamp(),
                     var versionsUrl: String? = null,
                     @Embedded var auth: Auth = Auth(),
                     @Id @GeneratedValue var id: Long? = null)

/**
 * Tokens for authorization on OCPI party servers
 * - tokenA = generated by admin; used in registration to broker
 * - tokenB = generated by party; used by broker as authorization on party's server
 * - tokenC = generated by broker; used by party for subsequent requests on broker's server
 */
@Embeddable
class Auth(var tokenA: String? = generateUUIDv4Token(),
           var tokenB: String? = null,
           var tokenC: String? = null)


/**
 * Store a role linked to an OCPI platform (i.e. a platform can implement both EMSP and CPO roles)
 */
@Entity
@Table(name = "roles")
class RoleEntity(var platformID: Long,
                 @Enumerated(EnumType.STRING) var role: Role,
                 @Embedded var businessDetails: BusinessDetails,
                 var partyID: String,
                 var countryCode: String,
                 @Id @GeneratedValue var id: Long? = null)


/**
 * Store endpoints associated with an OCPI platform retreived during the Versions/Credentials registration handshake
 */
@Entity
@Table(name = "endpoints")
class EndpointEntity(var platformID: Long,
                     var identifier: String,
                     @Enumerated(EnumType.STRING) var role: InterfaceRole,
                     var url: String,
                     @Id @GeneratedValue var id: Long? = null)


/**
 * Store a resource (URL) which will be proxied by the controller of the request
 */
@Entity
@Table(name = "proxy_resources")
class ProxyResourceEntity(
        @AttributeOverrides(
                AttributeOverride(name = "id", column = Column(name ="sender_id")),
                AttributeOverride(name = "country", column = Column(name ="sender_country"))
        )
        @Embedded
        val sender: BasicRole,

        @AttributeOverrides(
                AttributeOverride(name = "id", column = Column(name = "receiver_id")),
                AttributeOverride(name = "country", column = Column(name = "receiver_country"))
        )
        @Embedded
        var receiver: BasicRole,

        val resource: String,

        val alternativeUID: String? = null,

        @Id @GeneratedValue var id: Long? = null)
