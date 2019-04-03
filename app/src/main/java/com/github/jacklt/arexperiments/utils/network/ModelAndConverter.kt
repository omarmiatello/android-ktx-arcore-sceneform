package com.github.jacklt.arexperiments.utils.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlin.reflect.KClass

class Converter<T : Communication>(val type: KClass<T>, val serializer: KSerializer<T>?) {
    fun toObj(bytes: ByteArray) = serializer?.let { Cbor.load(it, bytes) } ?: type.objectInstance!!
    fun toBytes(obj: T) =  serializer?.let { Cbor.dump(it, obj) } ?: byteArrayOf()
    override fun toString() = "Converter<${type.java.simpleName}>"
}

inline fun <reified T : Communication> converter(serializer: KSerializer<T>?) =
    Converter(T::class, serializer)

sealed class Communication

sealed class Request<T : Response> : Communication() {
    @Serializable
    class Version(val currentVersion: String) : Request<Response.Version>()

    @Serializable
    object PING : Request<Response.PONG>()

    @Serializable
    data class MSG(val msg: String) : Request<Response.NoResponse>()

    @Serializable
    data class CanYouStartGame(val id: Byte) : Request<Response.StartGame>()

    @Serializable
    data class GetYourGameStatus(val id: Byte) : Request<Response.GameStatus>()
}

sealed class Response : Communication() {
    @Serializable
    object NoResponse : Response()

    @Serializable
    data class Version(val isSupported: Boolean) : Response()

    @Serializable
    object PONG : Response()

    @Serializable
    data class StartGame(val userChooseStart: Boolean) : Response()

    @Serializable
    data class GameStatus(
        val px: Float, val py: Float, val pz: Float,
        val dx: Float, val dy: Float, val dz: Float
    ) : Response()
}

