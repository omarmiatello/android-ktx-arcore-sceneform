package com.github.jacklt.arexperiments.utils

import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import java.util.concurrent.TimeUnit

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
