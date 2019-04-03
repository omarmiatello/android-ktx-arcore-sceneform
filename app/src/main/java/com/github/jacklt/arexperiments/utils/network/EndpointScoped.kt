package com.github.jacklt.arexperiments.utils.network

import androidx.lifecycle.MutableLiveData
import com.github.jacklt.arexperiments.generic.logD
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlin.system.measureTimeMillis

class EndpointScoped(val connection: EasyNearbyConnection, val endpointId: String, val nick: String) : CoroutineScope {
    private var job = Job()
    override val coroutineContext = Dispatchers.Default + job

    val incomingRequests = Channel<Request<*>>()
    val waitingResponses = mutableMapOf<String, Channel<out Response>>()
    val openChannels = mutableMapOf<String, Channel<out Response>>()
    val latency = MutableLiveData<Int>().apply { value = 0 }

    init {
        launch {
            while (true) {
                delay(5000)
                measureTimeMillis {
                    send(Request.PING).await()
                }.also {
                    latency.postValue(it.toInt())
                    logD("Latency with $endpointId: $it ms")
                }
            }
        }
    }

    fun onReceive(communication: Communication) {
        val requestId = communication::class.java.canonicalName!!
        when (communication) {
            is Request<*> -> {
                when (communication) {
                    Request.PING -> send(Response.PONG)
                    is Request.Version -> send(Response.Version(communication.currentVersion == EasyNearbyConnection.apiVersion))
                    is Request.MSG -> logD("Message from $endpointId: ${communication.msg}")
                    else -> launch { incomingRequests.send(communication) }
                }
            }
            is Response -> {
                val channel = waitingResponses[requestId] as? Channel<Response>
                val channelOpen = openChannels[requestId] as? Channel<Response>
                when {
                    channel != null -> {
                        channel.offer(communication)
                        channel.close()
                        waitingResponses.remove(requestId)
                    }
                    channelOpen != null -> {
                        logD("Found openChannel for: $requestId")
                        channelOpen.offer(communication)
                    }
                    else -> logD("Response without a channel: $communication")
                }
            }
        }!! // exhaustive
    }

    inline fun <reified T : Response> send(request: Request<T>): Deferred<T> = async {
        connection.sendPayload(endpointId, request)
        if (T::class == Response.NoResponse::class) {
            Response.NoResponse as T
        } else {
            val requestId = T::class.java.canonicalName!! // TODO improve, possible collision
            Channel<T>().also { waitingResponses[requestId] = it }.receive()
        }
    }

    inline fun <reified T : Response> sendGetReceiveChannel(request: Request<T>): Deferred<ReceiveChannel<T>> = async {
        connection.sendPayload(endpointId, request)
        val requestId = T::class.java.canonicalName!! // TODO improve, possible collision
        Channel<T>(Channel.CONFLATED).also { openChannels[requestId] = it }
    }

    fun send(response: Response) = async {
        connection.sendPayload(endpointId, response)
    }

    fun onCleared() {
        job.cancel()
        incomingRequests.close()
        waitingResponses.values.forEach { it.close() }
        openChannels.values.forEach { it.close() }
        logD("FINISH: EndpointScoped $endpointId")
    }

    interface Lifecycle {
        fun onConnect(endpoint: EndpointScoped, advertise: Boolean)
        fun onDisconnect(endpoint: EndpointScoped, advertise: Boolean)
    }

    companion object {
        fun onEndpointConnect(onConnect: (EndpointScoped, Boolean) -> Unit) = object : Lifecycle {
            override fun onConnect(endpoint: EndpointScoped, advertise: Boolean) = onConnect(endpoint, advertise)
            override fun onDisconnect(endpoint: EndpointScoped, advertise: Boolean) {}
        }
    }
}