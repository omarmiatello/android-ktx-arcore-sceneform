package com.github.jacklt.arexperiments

import com.github.jacklt.arexperiments.ar.addOnUpdateInMills
import com.github.jacklt.arexperiments.ar.distanceToColor
import com.github.jacklt.arexperiments.ar.floatAnimator
import com.github.jacklt.arexperiments.ar.minus
import com.github.jacklt.arexperiments.generic.SceneformActivity
import com.google.ar.sceneform.NodeParent
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory

class Scene1Activity : SceneformActivity() {
    override fun onInitSceneform() {
        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            hitResult.anchorNode {
                transformableNode {
                    box(0.1f, 0.03f, 0.2f, 0.01f, material(Color(1f, 0f, 0f, .2f)))
                    ball(material(Color(1f, 0f, 0f)))
                }
            }
        }
    }

    private suspend inline fun NodeParent.box(
        width: Float,
        height: Float,
        depth: Float,
        thick: Float,
        material: Material
    ) = node {
        // left
        node().renderable = ShapeFactory
            .makeCube(Vector3(thick, height, depth), Vector3((thick - width) / 2, height / 2, 0f), material)

        // right
        node().renderable = ShapeFactory
            .makeCube(Vector3(thick, height, depth), Vector3((width - thick) / 2, height / 2, 0f), material)

        // top
        node().renderable = ShapeFactory
            .makeCube(Vector3(width, height, thick), Vector3(0.0f, height / 2, (thick - depth) / 2), material)

        // bottom
        node().renderable = ShapeFactory
            .makeCube(Vector3(width, height, thick), Vector3(0.0f, height / 2, (depth - thick) / 2), material)
    }

    private suspend fun NodeParent.ball(red: Material) = node {
        renderable = ShapeFactory.makeSphere(0.01f, Vector3(0.0f, 0.01f, 0.0f), red)

        val xMax = (0.1f - (0.01f * 2) - (0.01f * 2)) / 2
        val yMax = (0.03f - (0.01f * 2) - (0.01f * 2)) / 2
        val zMax = (0.2f - (0.01f * 2) - (0.01f * 2)) / 2

        val xAnim = floatAnimator(0f, xMax, 0f, -xMax, 0f) { duration = 6000 }
        val yAnim = floatAnimator(0f, yMax, 0f) { duration = 2500 }
        val zAnim = floatAnimator(0f, zMax, 0f, -zMax, 0f) { duration = 4000 }

        val camera = arSceneView.scene.camera
        addOnUpdateInMills {
            val distance = (camera.worldPosition - worldPosition).length()
            renderable.material.setFloat3(MaterialFactory.MATERIAL_COLOR, distance.distanceToColor())
            localPosition = Vector3(xAnim.value(it), yAnim.value(it), zAnim.value(it))
        }
    }
}
