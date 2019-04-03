package com.github.jacklt.arexperiments.generic

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

abstract class NearbyActivity : AppCompatActivity() {
    abstract fun onNearbyReady()

    private fun hasNearbyPermission() = NEARBY_PERMISSIONS
        .all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

    override fun onStart() {
        super.onStart()
        if (hasNearbyPermission()) onNearbyReady() else requestPermissions(NEARBY_PERMISSIONS, REQ_NEARBY_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_NEARBY_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                onNearbyReady()
            } else {
                Toast.makeText(this, "Missing permissions", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    companion object {
        private val NEARBY_PERMISSIONS = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        private const val REQ_NEARBY_PERMISSIONS = 1
    }
}