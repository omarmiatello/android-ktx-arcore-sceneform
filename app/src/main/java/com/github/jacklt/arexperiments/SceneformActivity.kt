package com.github.jacklt.arexperiments

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.HitResult
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.NodeParent
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.coroutines.*
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.*

abstract class SceneformActivity : AppCompatActivity(), CoroutineScope {
    protected lateinit var job: Job
        private set

    protected lateinit var arFragment: ArFragment
        private set

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
        setContentView(R.layout.activity_main)
        arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment
        onInitSceneform()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel() // Cancel job on activity destroy. After destroy all children jobs will be cancelled automatically
    }

    abstract fun onInitSceneform()

    suspend fun <T> CompletableFuture<T>.await(): T = suspendCoroutine { cont: Continuation<T> ->
        whenComplete { result, exception ->
            if (exception == null) cont.resume(result) else cont.resumeWithException(exception)
        }
    }

    fun HitResult.anchorNode(init: suspend AnchorNode.() -> Unit): AnchorNode {
        return AnchorNode(createAnchor()).apply {
            setParent(arFragment.arSceneView.scene)
            launch { init(this@apply) }
        }
    }

    suspend fun material(color: Color): Material =
        if (color.a == 1f) {
            MaterialFactory.makeOpaqueWithColor(this, color).await()
        } else {
            MaterialFactory.makeTransparentWithColor(this, color).await()
        }


    suspend fun AnchorNode.transformableNode(init: suspend TransformableNode.() -> Unit): TransformableNode {
        return TransformableNode(arFragment.transformationSystem).apply {
            setParent(this@transformableNode)
            coroutineScope { init(this@apply) }
        }
    }

    suspend fun NodeParent.node(init: suspend Node.() -> Unit = {}): Node {
        return Node().apply {
            setParent(this@node)
            coroutineScope { init(this@apply) }
        }
    }

    suspend inline fun NodeParent.box(
        width: Float,
        height: Float,
        depth: Float,
        thick: Float,
        material: Material
    ) = node {
        // left
        node().renderable = ShapeFactory.makeCube(
            Vector3(thick, height, depth),
            Vector3((thick - width) / 2, height / 2, 0f),
            material
        )

        // right
        node().renderable = ShapeFactory.makeCube(
            Vector3(thick, height, depth),
            Vector3((width - thick) / 2, height / 2, 0f),
            material
        )

        // top
        node().renderable = ShapeFactory.makeCube(
            Vector3(width, height, thick),
            Vector3(0.0f, height / 2, (thick - depth) / 2),
            material
        )

        // bottom
        node().renderable = ShapeFactory.makeCube(
            Vector3(width, height, thick),
            Vector3(0.0f, height / 2, (depth - thick) / 2),
            material
        )
    }
}
