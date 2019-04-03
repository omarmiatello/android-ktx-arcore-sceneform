package com.github.jacklt.arexperiments.utils.network

import android.content.Context
import com.github.jacklt.arexperiments.generic.logD
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializerByClass
import kotlin.random.Random

class EasyNearbyConnection(
    context: Context,
    val nick: String,
    val maxConnections: Int = 1,
    val endpointLifecycle: EndpointScoped.Lifecycle
) : CoroutineScope {
    private var job = Job()
    override val coroutineContext = Dispatchers.Default + job

    // config
    private val applicationContext = context.applicationContext
    private val connectionsClient = Nearby.getConnectionsClient(applicationContext)
    private val serviceId = applicationContext.packageName + apiVersion     // TODO remove for add "backward compatible"
    private val advertisingOptions = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
    private val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()

    private val endpointsNearby = mutableSetOf<String>()
    private val endpointsConnected = mutableMapOf<String, EndpointScoped>()
    private val endpointNick = mutableMapOf<String, String>()
    private val lookingForConnection get() = endpointsConnected.size < maxConnections

    init {
        logD("EasyNearbyConnection START")
        launch {
            if (Random.nextBoolean()) advertiseRuled(3000..4000L)
            while (true) {
                discoverRuled(2500..3500L)
                advertiseRuled(3500..5500L)
            }
        }
    }

    private suspend fun advertiseRuled(duration: LongRange) {
        if (lookingForConnection) {
            logD("EasyNearbyConnection startAdvertising")
            connectionsClient.startAdvertising(nick, serviceId, onConnectionLifecycleAdvertise, advertisingOptions).await()
        }
        delay(Random.nextLong(duration.start, duration.endInclusive))
        connectionsClient.stopAdvertising()
    }

    private suspend fun discoverRuled(duration: LongRange) {
        if (lookingForConnection) {
            logD("EasyNearbyConnection startDiscovery")
            connectionsClient.startDiscovery(serviceId, onDiscovery, discoveryOptions).await()
        }
        delay(Random.nextLong(duration.start, duration.endInclusive))
        connectionsClient.stopDiscovery()
    }

    suspend fun sendPayload(endpointId: String, communication: Communication) {
        logD(
            when (communication) {
                is Request<*> -> "--> [${endpointNick[endpointId]}/$endpointId] $communication"
                is Response -> "${endpointNick[endpointId]}/$endpointId: --> $communication"
            }
        )
        connectionsClient.sendPayload(endpointId, communication.toPayload()).await()
    }

    private val onDiscovery = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            logD("EasyNearbyConnection onEndpointFound ${info.endpointName}/$endpointId")
            endpointsNearby += endpointId
            endpointNick[endpointId] = info.endpointName
            connectionsClient.requestConnection(nick, endpointId, onConnectionLifecycleDiscover)
        }

        override fun onEndpointLost(endpointId: String) {
            logD("EasyNearbyConnection onEndpointLost ${endpointNick[endpointId]}/$endpointId")
            endpointsNearby -= endpointId
        }
    }

    private val onConnectionLifecycleAdvertise = connectionLifecycleCallback(advertise = true)
    private val onConnectionLifecycleDiscover = connectionLifecycleCallback(advertise = false)

    private fun connectionLifecycleCallback(advertise: Boolean) = object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                logD("EasyNearbyConnection onConnectionInitiated ${info.endpointName}/$endpointId")
                connectionsClient.acceptConnection(endpointId, onPayload)
                endpointNick[endpointId] = info.endpointName
            }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                logD("EasyNearbyConnection onConnectionResult ${endpointNick[endpointId]}/$endpointId status: ${result.status.statusMessage} (${result.status.isSuccess})")
                if (result.status.isSuccess) {
                    val endpoint = EndpointScoped(this@EasyNearbyConnection, endpointId, endpointNick[endpointId]!!)
                    endpointsConnected[endpointId] = endpoint
                    if (!lookingForConnection) {
                        logD("EasyNearbyConnection stopDiscovery() / stopAdvertising()")
                        connectionsClient.stopDiscovery()
                        connectionsClient.stopAdvertising()
                    }
                    endpointLifecycle.onConnect(endpoint, advertise)
                }
            }

            override fun onDisconnected(endpointId: String) {
                logD("EasyNearbyConnection onDisconnected ${endpointNick[endpointId]}/$endpointId")
                endpointsConnected.remove(endpointId)?.also {
                    endpointLifecycle.onDisconnect(it, advertise)
                    it.onCleared()
                }
            }
        }

    private val onPayload = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val endpoint = requireNotNull(endpointsConnected[endpointId]) { "Endpoint $endpointId not connected" }
            val communication = payload.toCommunication()
            logD(
                when (communication) {
                    is Request<*> -> "${endpointNick[endpointId]}/$endpointId: <-- $communication"
                    is Response -> " <-- [${endpointNick[endpointId]}/$endpointId] $communication"
                }
            )
            endpoint.onReceive(communication)
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Bytes payloads are sent as a single chunk, so you'll receive a SUCCESS update immediately
            // after the call to onPayloadReceived().
        }
    }

    fun onCleared() {
        job.cancel()
        endpointsConnected.forEach { _, e -> e.onCleared() }
        connectionsClient.stopDiscovery()
        connectionsClient.stopAdvertising()
        connectionsClient.stopAllEndpoints()
        logD("EasyNearbyConnection FINISH")
    }

    companion object {
        val apiVersion = "v0.0.1"   // TODO remove, now is ALWAYS backward NOT compatible (for faster iteration)

        private val converters = listOf(
            converter(Request.Version.serializer()),
            converter<Request.PING>(null),
            converter(Request.MSG.serializer()),
            converter(Request.CanYouStartGame.serializer()),
            converter(Request.GetYourGameStatus.serializer()),
            converter(Response.Version.serializer()),
            converter<Response.PONG>(null),
            converter(Response.StartGame.serializer()),
            converter(Response.GameStatus.serializer())
        )

        private fun Payload.toCommunication() = when (type) {
            Payload.Type.BYTES -> requireNotNull(asBytes()) { "Type $type. asBytes() should be NOT null" }.let { bytes ->
                converters[bytes[0].toInt()].toObj(bytes.copyOfRange(1, bytes.size))
            }
            else -> throw IllegalStateException("Type $type not handled (yet!). Would you like to add it?")
        }

        private fun Communication.toPayload(): Payload {
            val converter = converters.first { it.type == this::class } as Converter<Communication>
            val id = converters.indexOf(converter).toByte()
            return Payload.fromBytes(byteArrayOf(id, *converter.toBytes(this)))
        }
    }
}

