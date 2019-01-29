package com.github.jacklt.arexperiments

import com.github.jacklt.arexperiments.ar.*
import com.github.jacklt.arexperiments.generic.SceneformActivity
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

class Scene5Activity : SceneformActivity() {
    override fun onInitSceneform() {
        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            hitResult.anchorNode {
                transformableNode {
                    scaleController.apply {
                        minScale = .25f
                        maxScale = 4f
                    }
                    walls(.5f, 0.5f, 1f, 0.01f)
                    val ball = ball(.015f, material(Color(1f, 0f, 0f))).apply { localPosition = Vector3(0f, .3f, 0f) }

                    // Game mechanics
                    var lastNodeDirection: Node? = null
                    addOnUpdateInMills {
                        val cameraDistance = (scene.camera.worldPosition - ball.worldPosition).length()
                        if (cameraDistance < 0.3) {
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
                                        "wall forward" -> Vector3.back()
                                        "wall back" -> Vector3.forward()
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
                                ball.currentMovement = ball.localPositionAnimator(point).apply {
                                    duration = (2000 * distance).toLong()
                                    start()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun NodeParent.walls(width: Float, height: Float, depth: Float, thick: Float) = node("walls") {
        val wallMaterial = material(Color(0f, 0f, 1f, .5f)).apply {
            setReflectance(0f)
            setRoughness(1f)
        }

        node("wall left").renderable = ShapeFactory
            .makeCube(Vector3(thick, height, depth), Vector3(-width / 2, 0f, 0f), wallMaterial).noShadow()

        node("wall right").renderable = ShapeFactory
            .makeCube(Vector3(thick, height, depth), Vector3(width / 2, 0f, 0f), wallMaterial).noShadow()

        node("wall up").collisionShape = Box(Vector3(width, thick, depth), Vector3(0f, height / 2, 0f))
        node("wall down").collisionShape = Box(Vector3(width, thick, depth), Vector3(0f, -height / 2, 0f))
        node("wall forward").collisionShape = Box(Vector3(width, height, thick), Vector3(0f, 0f, -depth / 2))
        node("wall back").collisionShape = Box(Vector3(width, height, thick), Vector3(0f, 0f, depth / 2))

        localPosition = Vector3(0f, height / 2, 0f)
    }

    private suspend fun NodeParent.ball(radius: Float, material: Material) = nodeAnimated("ball") {
        renderable = ShapeFactory.makeSphere(radius, Vector3(0f, 0f, 0f), material.apply {
            setMetallic(1f)
            setRoughness(0f)
        })
        currentDirection = randomVector3normalized()

        addOnUpdateInMills {
            val cameraDistance = (scene.camera.worldPosition - worldPosition).length()
            renderable.material.setFloat3(MaterialFactory.MATERIAL_COLOR, cameraDistance.distanceToColor())
        }
    }
}

