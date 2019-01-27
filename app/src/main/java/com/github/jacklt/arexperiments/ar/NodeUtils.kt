package com.github.jacklt.arexperiments.ar

import android.animation.ObjectAnimator
import com.github.jacklt.arexperiments.generic.logD
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Renderable
import java.util.concurrent.TimeUnit
import kotlin.math.abs

fun Node.addOnUpdateInMills(onUpdate: (Long) -> Unit) {
    addLifecycleListener(object : Node.LifecycleListener {
        override fun onDeactivated(p0: Node?) {
        }

        override fun onActivated(p0: Node?) {
        }

        override fun onUpdated(p0: Node?, p1: FrameTime) {
            onUpdate(p1.getStartTime(TimeUnit.MILLISECONDS))
        }
    })
}

fun Node.localPositionAnimator(vararg values: Vector3) = vectorAnimator(
    "localPosition",
    arrayOf(localPosition, *values)
).apply {
    logD("localPositionAnimator: $localPosition ${values.toList()}")
    target = this@localPositionAnimator
}

fun Float.distanceToColor() = Color(
    1f - (abs(this - 0f) * 3f - 0.2f).coerceIn(0f..1f),
    1f - (abs(this - 0.5f) * 3f - 0.2f).coerceIn(0f..1f),
    1f - (abs(this - 1f) * 3f - 0.2f).coerceIn(0f..1f)
)

class NodeAnimated : Node() {
    var currentMovement = ObjectAnimator()
    var currentDirection = Vector3.forward()
}

fun Renderable.noShadow() = apply {
    isShadowCaster = false
    isShadowReceiver = false
}