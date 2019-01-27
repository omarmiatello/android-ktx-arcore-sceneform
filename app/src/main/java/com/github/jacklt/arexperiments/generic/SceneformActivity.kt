package com.github.jacklt.arexperiments.generic

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.jacklt.arexperiments.R
import com.github.jacklt.arexperiments.ar.NodeAnimated
import com.google.ar.core.HitResult
import com.google.ar.core.Pose
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.NodeParent
import com.google.ar.sceneform.rendering.*
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

    protected lateinit var arSceneView: ArSceneView
        private set

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        job = Job()
        setContentView(R.layout.activity_sceneform)
        arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment
        arSceneView = arFragment.arSceneView
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

    suspend fun View.toViewRenderable(builder: (ViewRenderable.Builder) -> Unit = {}) =
        ViewRenderable.builder().setView(context, this).also(builder).build().await()

    suspend fun material(color: Color): Material =
        if (color.a == 1f) {
            MaterialFactory.makeOpaqueWithColor(this, color).await()
        } else {
            MaterialFactory.makeTransparentWithColor(this, color).await()
        }

    suspend fun material(texture: Texture): Material = TODO()

    fun Pose.anchorNode(init: suspend AnchorNode.() -> Unit): AnchorNode {
        return AnchorNode(arSceneView.session.createAnchor(this)).apply {
            setParent(arSceneView.scene)
            launch { init(this@apply) }
        }
    }

    fun HitResult.anchorNode(init: suspend AnchorNode.() -> Unit): AnchorNode {
        return AnchorNode(createAnchor()).apply {
            setParent(arSceneView.scene)
            launch { init(this@apply) }
        }
    }

    suspend fun AnchorNode.transformableNode(init: suspend TransformableNode.() -> Unit): TransformableNode {
        return TransformableNode(arFragment.transformationSystem).apply {
            setParent(this@transformableNode)
            coroutineScope { init(this@apply) }
        }
    }

    suspend fun NodeParent.node(name: String? = null, init: suspend Node.() -> Unit = {}): Node {
        return Node().apply {
            if (name != null) setName(name)
            setParent(this@node)
            coroutineScope { init(this@apply) }
        }
    }

    suspend fun NodeParent.nodeAnimated(name: String? = null, init: suspend NodeAnimated.() -> Unit = {}): NodeAnimated {
        return NodeAnimated().apply {
            if (name != null) setName(name)
            setParent(this@nodeAnimated)
            coroutineScope { init(this@apply) }
        }
    }
}