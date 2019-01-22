package com.github.jacklt.arexperiments.utils

import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator

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

