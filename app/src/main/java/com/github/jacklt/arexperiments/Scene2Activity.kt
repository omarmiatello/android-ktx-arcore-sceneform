package com.github.jacklt.arexperiments

import com.github.jacklt.arexperiments.ar.addOnUpdateInMills
import com.github.jacklt.arexperiments.ar.distanceToColor
import com.github.jacklt.arexperiments.ar.localPositionAnimator
import com.github.jacklt.arexperiments.ar.minus
import com.github.jacklt.arexperiments.generic.SceneformActivity
import com.google.ar.sceneform.NodeParent
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory

class Scene2Activity : SceneformActivity() {
    override fun onInitSceneform() {
        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            hitResult.anchorNode {
                transformableNode {
                    parallelPlanes(1f, 0.8f, 1f, 0.01f, material(Color(0f, 0f, 1f, .2f)))
                    ball(material(Color(1f, 0f, 0f))).apply {
                        localPosition = Vector3(0f, .3f, 0f)
                    }
                }
            }
        }
    }

    private suspend fun NodeParent.parallelPlanes(
        width: Float,
        height: Float,
        depth: Float,
        thick: Float,
        material: Material
    ) = node {
        // left
        node().renderable = ShapeFactory
            .makeCube(Vector3(thick, height, depth), Vector3((thick - width) / 2, height / 2, 0f), material)
            .apply {
                isShadowCaster = false
                isShadowReceiver = false
            }

        // right
        node().renderable = ShapeFactory
            .makeCube(Vector3(thick, height, depth), Vector3((width - thick) / 2, height / 2, 0f), material)
            .apply {
                isShadowCaster = false
                isShadowReceiver = false
            }
    }

    private suspend fun NodeParent.ball(red: Material) = node {
        renderable = ShapeFactory.makeSphere(0.01f, Vector3(0.0f, 0.0f, 0.0f), red)

        val camera = arSceneView.scene.camera
        addOnUpdateInMills {
            val distance = (camera.worldPosition - worldPosition).length()
            renderable.material.setFloat3(MaterialFactory.MATERIAL_COLOR, distance.distanceToColor())

            if (distance < 0.3) {
                // WITHOUT animation
                // localPosition += camera.forward * 0.3f

                // WITH animation
                localPositionAnimator(camera.forward).apply { duration = 500 }.start()
            }
        }
    }
}

