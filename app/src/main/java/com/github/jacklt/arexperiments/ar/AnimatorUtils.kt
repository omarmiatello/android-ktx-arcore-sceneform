package com.github.jacklt.arexperiments.ar

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.QuaternionEvaluator
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.math.Vector3Evaluator


class FloatAnimator private constructor(val valueAnimator: ValueAnimator) {
    fun value(millis: Long): Float {
        valueAnimator.currentPlayTime = millis
        return valueAnimator.animatedValue as Float
    }

    companion object {
        fun from(values: FloatArray, init: ValueAnimator.() -> Unit = {}) =
            FloatAnimator(ValueAnimator.ofFloat(*values)).also {
                it.valueAnimator.apply {
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = LinearInterpolator()
                }
                init(it.valueAnimator)
            }
    }
}

inline fun floatAnimator(vararg values: Float, noinline init: ValueAnimator.() -> Unit = {}) =
    FloatAnimator.from(values, init)

fun vectorAnimator(property: String, values: Array<Vector3>) = ObjectAnimator().apply {
    setObjectValues(*values)
    propertyName = property
    interpolator = LinearInterpolator()
    setEvaluator(Vector3Evaluator())
    setAutoCancel(true)
}

fun quaternionAnimator(property: String, values: Array<Quaternion>) = ObjectAnimator().apply {
    setObjectValues(*values)
    propertyName = property
    setEvaluator(QuaternionEvaluator())

    //  Allow orbitAnimation to repeat forever
    repeatCount = ObjectAnimator.INFINITE
    repeatMode = ObjectAnimator.RESTART
    interpolator = LinearInterpolator()
    setAutoCancel(true)
}
