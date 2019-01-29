package com.github.jacklt.arexperiments

import com.github.jacklt.arexperiments.ar.*
import com.github.jacklt.arexperiments.generic.SceneformActivity
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.ShapeFactory

class Scene3Activity : SceneformActivity() {
    override fun onInitSceneform() {
        arFragment.planeDiscoveryController.apply {
            hide()
            setInstructionView(null)
        }
        val camera = arSceneView.scene.camera
        var balls: Node? = null
        arSceneView.scene.addOnUpdateListener {
            if (arSceneView.arFrame.camera.trackingState != TrackingState.TRACKING) return@addOnUpdateListener

            // Place the anchor 50cm in front of the camera.
            when {
                arSceneView.session.allAnchors.isEmpty() -> {
                    val pos = floatArrayOf(0f, 0f, -.05f)
                    val rotation = floatArrayOf(0f, 0f, 0f, 1f)
                    Pose(pos, rotation).anchorNode {
                        balls = node {
                            node {
                                renderable = ShapeFactory.makeSphere(0.05f, Vector3(0.0f, -0.13f, 0.0f),
                                    material(Color(1f, 0f, 0f)))
                            }
                            node {
                                renderable = ShapeFactory.makeSphere(0.05f, Vector3(0.0f, 0.0f, 0.0f),
                                    material(Color(1f, 0f, 0f)).apply {
                                        setRoughness(0f)
                                        setReflectance(1f)
                                    })
                            }
                            node {
                                renderable = ShapeFactory.makeSphere(0.05f, Vector3(0.0f, 0.13f, 0.0f),
                                    material(Color(1f, 0f, 0f)).apply {
                                        setRoughness(0f)
                                        setMetallic(1f)
                                    })
                            }
                        }
                    }
                }
                balls != null -> {
                    balls!!.worldPosition = camera.worldPosition + camera.forward * 0.5f
                    balls!!.worldRotation = camera.worldRotation
                }
            }
        }
    }
}

