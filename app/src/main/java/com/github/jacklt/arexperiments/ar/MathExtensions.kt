package com.github.jacklt.arexperiments.ar

import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3


// Vector3 extensions

inline operator fun Vector3.plus(other: Vector3) = Vector3.add(this, other)
inline operator fun Vector3.minus(other: Vector3) = Vector3.subtract(this, other)
// inline operator fun Vector3.times(other: Vector3) =  Vector3.dot(this, other)
inline operator fun Vector3.times(value: Float) = if (value == -1f) negated() else scaled(value)

fun Vector3.reflect(normal: Vector3): Vector3 {
    // I is the original array
    // N is the normal of the incident plane
    // R = I - (2 * N * ( DotProduct[I,N] ))

    return this - normal * 2f * Vector3.dot(this, normal)
}


// Quaternion extensions

inline operator fun Quaternion.times(other: Quaternion) = Quaternion.multiply(this, other)
