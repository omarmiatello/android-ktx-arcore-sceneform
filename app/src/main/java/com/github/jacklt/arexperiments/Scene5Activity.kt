package com.github.jacklt.arexperiments

import com.github.jacklt.arexperiments.ar.*
import com.github.jacklt.arexperiments.generic.SceneformActivity
import com.google.ar.sceneform.NodeParent
import com.google.ar.sceneform.collision.Box
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
                    walls(1f, 0.8f, 2f, 0.01f, material(Color(0f, 0f, 1f, .2f)))
                    ball(.015f, material(Color(1f, 0f, 0f))).apply { localPosition = Vector3(0f, .3f, 0f) }

                    // Game mechanics
                    // TODO WIP
                }
            }
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

