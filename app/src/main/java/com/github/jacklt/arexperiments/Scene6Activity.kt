package com.github.jacklt.arexperiments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProviders
import com.github.jacklt.arexperiments.ar.*
import com.github.jacklt.arexperiments.databinding.ArviewScene6PlayercardBinding
import com.github.jacklt.arexperiments.generic.SceneformActivity
import com.github.jacklt.arexperiments.generic.logD
import com.github.jacklt.arexperiments.utils.ViewModelScoped
import com.github.jacklt.arexperiments.utils.network.EasyNearbyConnection
import com.github.jacklt.arexperiments.utils.network.EndpointScoped
import com.github.jacklt.arexperiments.utils.network.Request
import com.github.jacklt.arexperiments.utils.network.Response
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.NodeParent
import com.google.ar.sceneform.collision.Box
import com.google.ar.sceneform.collision.Ray
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.random.Random


class Scene6Activity : SceneformActivity() {

    override fun onInitSceneform() {
        val gameStatus = ViewModelProviders.of(this).get(GameStatusViewModel::class.java)

        val (player1, player2) = gameStatus.run {
            val nick = "Player " + Random.nextInt(100)
            start(this@Scene6Activity, nick)
            player1.nick.value = nick
            player1 to player2
        }
        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            hitResult.anchorNode {
                transformableNode {
                    playerCards(player1, player2).apply { localPosition = Vector3(0f, 0f, -2f) }

                    walls(1f, .8f, 1.5f, 0.01f, material(Color(0f, 0f, 1f, .2f)))

                    val ball = ball(.015f, material(Color(1f, 0f, 0f))).apply { localPosition = Vector3(0f, .3f, 0f) }

                    // Game mechanics

                    var lastNodeDirection: Node? = null
                    addOnUpdateInMills {
                        val cameraDistance = (scene!!.camera.worldPosition - ball.worldPosition).length()
                        if (cameraDistance < 0.3) {
                            logD("ball -> camera distance: $cameraDistance")
                            ball.currentMovement.cancel()
                            lastNodeDirection = null
                            val newDirection = scene!!.camera.forward
                            ball.currentDirection = worldToLocalDirection(newDirection)
                            val newPosition = ball.localPosition
                            gameStatus.currentStatus.offer(
                                Response.GameStatus(
                                    newPosition.x,
                                    newPosition.y,
                                    newPosition.z,
                                    newDirection.x,
                                    newDirection.y,
                                    newDirection.z
                                )
                            )
                        }

                        if (gameStatus.hasGame && !ball.currentMovement.isRunning) {
                            if (lastNodeDirection != null) {
                                ball.currentDirection = ball.currentDirection.reflect(
                                    when (lastNodeDirection!!.name) {
                                        "wall left" -> Vector3.right()
                                        "wall right" -> Vector3.left()
                                        "wall forward" -> {
                                            player1.points.value = (player1.points.value ?: 0) + 1
                                            Vector3.back()
                                        }
                                        "wall back" -> {
                                            player2.points.value = (player2.points.value ?: 0) + 1
                                            Vector3.forward()
                                        }
                                        "wall up" -> Vector3.down()
                                        "wall down" -> Vector3.up()
                                        else -> throw IllegalStateException("Unknown node ${lastNodeDirection!!.name}")
                                    }
                                ).normalized()
                            }
                            val hitTest =
                                scene!!.hitTestAll(
                                    Ray(
                                        ball.worldPosition,
                                        localToWorldDirection(ball.currentDirection)
                                    )
                                )
                                    .firstOrNull { it.node != ball && it.node != lastNodeDirection }
                            if (hitTest != null) {
                                lastNodeDirection = hitTest.node
                                val point = worldToLocalPoint(hitTest.point)
                                val distance = (ball.localPosition - point).length()
                                logD("ball -> animation hit: ${lastNodeDirection!!.name} $point (distance $distance)")
                                ball.currentMovement = ball.localPositionAnimator(point).apply {
                                    duration = (1000 * distance).toLong()
                                    start()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun NodeParent.playerCards(
        player1: PlayerStatus,
        player2: PlayerStatus
    ) = node("playerCards") {
        node("playerCard 1") {
            localPosition = Vector3(-0.3f, 0f, 0f)
            renderable = ArviewScene6PlayercardBinding.inflate(LayoutInflater.from(this@Scene6Activity))
                .apply {
                    lifecycleOwner = this@Scene6Activity
                    status = player1
                }
                .root.toViewRenderable().noShadow()
        }
        node("playerCard 2") {
            localPosition = Vector3(0.3f, 0f, 0f)
            renderable = ArviewScene6PlayercardBinding.inflate(LayoutInflater.from(this@Scene6Activity))
                .apply {
                    lifecycleOwner = this@Scene6Activity
                    status = player2
                }
                .root.toViewRenderable().noShadow()
        }
    }


    private suspend fun NodeParent.walls(
        width: Float,
        height: Float,
        depth: Float,
        thick: Float,
        material: Material
    ) = node("walls") {
        node("wall left").renderable = ShapeFactory
            .makeCube(Vector3(thick, height, depth), Vector3(-width / 2, 0f, 0f), material).noShadow()

        node("wall right").renderable = ShapeFactory
            .makeCube(Vector3(thick, height, depth), Vector3(width / 2, 0f, 0f), material).noShadow()

        node("wall up").renderable = ShapeFactory
            .makeCube(Vector3(width, thick, depth), Vector3(0f, height / 2, 0f), material).noShadow()

        node("wall down").renderable = ShapeFactory
            .makeCube(Vector3(width, thick, depth), Vector3(0f, -height / 2, 0f), material).noShadow()

        node("wall forward").collisionShape =
            Box(Vector3(width, height, thick), Vector3(0f, 0f, -depth / 2))

        node("wall back").collisionShape =
            Box(Vector3(width, height, thick), Vector3(0f, 0f, depth / 2))

        localPosition = Vector3(0f, height / 2, 0f)
    }

    private suspend fun NodeParent.ball(radius: Float, material: Material) = nodeAnimated("ball") {
        renderable = ShapeFactory.makeSphere(radius, Vector3(0f, 0f, 0f), material)

        currentDirection =
            (Vector3.forward() * Random.nextFloat() + Vector3.right() * Random.nextFloat() + Vector3.up() * Random.nextFloat())
                .normalized()

        addOnUpdateInMills {
            val cameraDistance = (scene!!.camera.worldPosition - worldPosition).length()
            renderable!!.material.setFloat3(MaterialFactory.MATERIAL_COLOR, cameraDistance.distanceToColor())
        }
    }

    class PlayerStatus {
        val points = MutableLiveData<Int>().apply { value = 0 }
        val nick = MutableLiveData<String>()
    }

    class GameStatusViewModel : ViewModelScoped() {
        val query: MutableLiveData<String> = MutableLiveData()
        val player1 = PlayerStatus()
        val player2 = PlayerStatus()

        var currentGame: Byte = 0
        var currentStatus = Channel<Response.GameStatus>(Channel.CONFLATED)
        val hasGame get() = currentGame != 0.toByte()

        var easyNearbyConnection: EasyNearbyConnection? = null

        fun start(context: Context, nick: String) {
            if (easyNearbyConnection == null) {
                easyNearbyConnection = EasyNearbyConnection(
                    context,
                    nick,
                    endpointLifecycle = EndpointScoped.onEndpointConnect { endpoint, advertise ->
                        suspend fun receiveGameStatus(gameId: Byte) {
                            currentGame = gameId
                            player2.nick.value = endpoint.nick
                            val receiveChannel =
                                endpoint.sendGetReceiveChannel(Request.GetYourGameStatus(gameId)).await()
                            for (gameStatus in receiveChannel) {

                            }
                        }

                        launch {
                            for (request in endpoint.incomingRequests) {
                                when (request) {
                                    is Request.CanYouStartGame -> {
                                        launch {
                                            val userChooseStart = !hasGame
                                            endpoint.send(Response.StartGame(userChooseStart))
                                            if (userChooseStart) receiveGameStatus(request.id)
                                        }
                                    }
                                    is Request.GetYourGameStatus -> {
                                        launch {
                                            for (status in currentStatus) {
                                                endpoint.send(status).await()
                                            }
                                        }
                                    }
                                    is Request.PING,
                                    is Request.Version,
                                    is Request.MSG -> error("Should not happen: $request")
                                }!! // exhaustive
                            }
                        }

                        if (advertise) {
                            launch {
                                if (!hasGame) {
                                    val gameRequest = Request.CanYouStartGame(1)
                                    val userChooseStart = endpoint.send(gameRequest).await().userChooseStart
                                    if (userChooseStart) receiveGameStatus(gameRequest.id)
                                }
                            }
                        }
                    })
            }
        }

        override fun onCleared() {
            easyNearbyConnection?.onCleared()
            easyNearbyConnection = null
            super.onCleared()
        }
    }

    override fun onStart() {
        super.onStart()
        checkNearbyPermission()
    }

    override fun onStop() {
//        connectionsClient.stopAllEndpoints()
//        resetGame()
        super.onStop()
    }

    // Permission

    private fun checkNearbyPermission() {
        val isOk = NEARBY_PERMISSIONS
            .all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
        if (!isOk) requestPermissions(NEARBY_PERMISSIONS, REQ_NEARBY_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_NEARBY_PERMISSIONS) {
            grantResults.forEach {
                if (it == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, "Missing permissions", Toast.LENGTH_LONG).show()
                    finish()
                    return
                }
            }
            recreate()
        }
    }

    companion object {
        private val NEARBY_PERMISSIONS = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        private const val REQ_NEARBY_PERMISSIONS = 1
    }
}

