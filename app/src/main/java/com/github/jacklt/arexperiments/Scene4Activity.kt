package com.github.jacklt.arexperiments

import android.view.LayoutInflater
import androidx.lifecycle.MutableLiveData
import com.github.jacklt.arexperiments.ar.*
import com.github.jacklt.arexperiments.databinding.ArviewScene4PlayercardBinding
import com.github.jacklt.arexperiments.generic.SceneformActivity
import com.github.jacklt.arexperiments.generic.SimpleItem
import com.github.jacklt.arexperiments.generic.logD
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.NodeParent
import com.google.ar.sceneform.collision.Box
import com.google.ar.sceneform.collision.Ray
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import kotlin.random.Random

class Scene4Activity : SceneformActivity() {
    class PlayerStatus {
        val points = MutableLiveData<Int>().apply { value = 0 }
    }

    override fun onInitSceneform() {
        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            val player1 = PlayerStatus()
            val player2 = PlayerStatus()
            hitResult.anchorNode {
                transformableNode {
                    playerCards(player1, player2).apply { localPosition = Vector3(0f, 0f, -2f) }

                    walls(1f, .8f, 1.5f, 0.01f, material(Color(0f, 0f, 1f, .2f)))

                    val ball = ball(.015f, material(Color(1f, 0f, 0f))).apply { localPosition = Vector3(0f, .3f, 0f) }

                    // Game mechanics

                    var lastNodeDirection: Node? = null
                    addOnUpdateInMills {
                        val cameraDistance = (scene.camera.worldPosition - ball.worldPosition).length()
                        if (cameraDistance < 0.3) {
                            logD("ball -> camera distance: $cameraDistance")
                            ball.currentMovement.cancel()
                            lastNodeDirection = null
                            ball.currentDirection = worldToLocalDirection(scene.camera.forward)
                        }

                        if (!ball.currentMovement.isRunning) {
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
                                scene.hitTestAll(Ray(ball.worldPosition, localToWorldDirection(ball.currentDirection)))
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
            renderable = ArviewScene4PlayercardBinding.inflate(LayoutInflater.from(this@Scene4Activity))
                .apply {
                    setLifecycleOwner(this@Scene4Activity)
                    status = player1
                    item = SimpleItem(description = "Player 1")
                }
                .root.toViewRenderable().noShadow()
        }
        node("playerCard 2") {
            localPosition = Vector3(0.3f, 0f, 0f)
            renderable = ArviewScene4PlayercardBinding.inflate(LayoutInflater.from(this@Scene4Activity))
                .apply {
                    setLifecycleOwner(this@Scene4Activity)
                    status = player2
                    item = SimpleItem(description = "Player 2")
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
            val cameraDistance = (scene.camera.worldPosition - worldPosition).length()
            renderable.material.setFloat3(MaterialFactory.MATERIAL_COLOR, cameraDistance.distanceToColor())
        }
    }
}

