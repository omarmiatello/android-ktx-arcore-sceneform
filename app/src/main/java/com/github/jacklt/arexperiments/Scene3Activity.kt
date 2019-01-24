package com.github.jacklt.arexperiments

import com.github.jacklt.arexperiments.ar.plus
import com.github.jacklt.arexperiments.generic.SceneformActivity
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.ShapeFactory
import kotlinx.coroutines.launch

class Scene3Activity : SceneformActivity() {
    override fun onInitSceneform() {
        arFragment.planeDiscoveryController.apply {
            hide()
            setInstructionView(null)
        }
        val camera = arSceneView.scene.camera
        var ball: Node? = null
        arSceneView.scene.addOnUpdateListener {
            if (arSceneView.arFrame.camera.trackingState != TrackingState.TRACKING) return@addOnUpdateListener

            // Place the anchor 50cm in front of the camera.
            when {
                arSceneView.session.allAnchors.isEmpty() -> {
                    val pos = floatArrayOf(0f, 0f, -.5f)
                    val rotation = floatArrayOf(0f, 0f, 0f, 1f)
                    val anchor = arSceneView.session.createAnchor(Pose(pos, rotation))
                    AnchorNode(anchor).apply {
                        launch {
                            ball = node {
                                val red = material(Color(1f, 0f, 0f))
                                renderable = ShapeFactory.makeSphere(0.1f, Vector3(0.0f, 0.0f, 0.0f), red)
                            }
                        }
                        setParent(arSceneView.scene)
                    }
                }
                ball != null -> {
                    ball!!.worldPosition = camera.worldPosition + camera.forward
                }
            }
        }
    }
}

