package com.github.jacklt.arexperiments

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.github.jacklt.arexperiments.databinding.ActivityMainBinding
import com.github.jacklt.arexperiments.generic.SimpleItem
import com.github.jacklt.arexperiments.generic.SimpleItemAdapter

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
            .recyclerView
            .adapter = SimpleItemAdapter(
            listOf(
                SimpleItem(
                    "Scene 1",
                    "onTapArPlane: box with animated ball",
                    ref = Scene1Activity::class.java
                ),
                SimpleItem(
                    "Scene 2",
                    "onTapArPlane: camera can push the ball",
                    ref = Scene2Activity::class.java
                ),
                SimpleItem(
                    "Scene 3",
                    "camera: ball at fixed distance",
                    ref = Scene3Activity::class.java
                ),
                SimpleItem(
                    "Scene 4",
                    "onTapArPlane: game pong like",
                    ref = Scene4Activity::class.java
                ),
                SimpleItem(
                    "Scene 5 (Work in progress)",
                    "onTapArPlane: materials experiment (+ game pong)",
                    ref = Scene5Activity::class.java
                )
            )
        ) { _, item -> startActivity(Intent(this, item.ref as Class<*>)) }
    }
}
