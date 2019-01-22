package com.github.jacklt.arexperiments

import com.github.jacklt.arexperiments.utils.addOnUpdateInMills
import com.github.jacklt.arexperiments.utils.floatAnimator
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import kotlin.math.abs

class MainActivity : SceneformActivity() {
    override fun onInitSceneform() {
        val camera = arFragment.arSceneView.scene.camera
        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            hitResult.anchorNode {
                val red = material(Color(1f, 0f, 0f))
                transformableNode {
                    scaleController.maxScale = 10f

                    box(0.1f, 0.03f, 0.2f, 0.01f, material(Color(1f, 0f, 0f, .2f)))

                    val ball = node { renderable = ShapeFactory.makeSphere(0.01f, Vector3(0.0f, 0.01f, 0.0f), red) }

                    val xMax = (0.1f - (0.01f * 2) - (0.01f * 2)) / 2
                    val yMax = (0.03f - (0.01f * 2) - (0.01f * 2)) / 2
                    val zMax = (0.2f - (0.01f * 2) - (0.01f * 2)) / 2

                    val xAnim = floatAnimator(0f, xMax, 0f, -xMax, 0f) { duration = 6000 }
                    val yAnim = floatAnimator(0f, yMax, 0f) { duration = 2500 }
                    val zAnim = floatAnimator(0f, zMax, 0f, -zMax, 0f) { duration = 4000 }

                    ball.addOnUpdateInMills {
                        val distance = Vector3.subtract(camera.worldPosition, ball.worldPosition).length()
                        val r = 1f - (abs(distance - 0f) * 3f - 0.2f).coerceIn(0f..1f)
                        val g = 1f - (abs(distance - 0.5f) * 3f - 0.2f).coerceIn(0f..1f)
                        val b = 1f - (abs(distance - 1f) * 3f - 0.2f).coerceIn(0f..1f)
                        ball.renderable.material.setFloat3(MaterialFactory.MATERIAL_COLOR, Color(r, g, b))
                        ball.localPosition = Vector3(xAnim.value(it), yAnim.value(it), zAnim.value(it))
                    }
                }
            }
        }
    }
}
