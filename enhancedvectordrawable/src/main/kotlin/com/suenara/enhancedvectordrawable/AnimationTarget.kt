package com.suenara.enhancedvectordrawable

import android.animation.PropertyValuesHolder
import androidx.annotation.Keep

@Keep
interface AnimationTarget {

    @Suppress("UNCHECKED_CAST", "unused")
    enum class Property(
        val tag: String,
        private val valueSetter: PropertyValuesHolder.(Array<out Any>) -> Unit
    ) {
        FILL_COLOR("fillColor", { array ->
            if (array.isPrimitive) {
                setIntValues(*(array as Array<Int>).toIntArray())
            } else {
                setIntValues(*array.filterIsInstance<Int>().toIntArray())
            }
        }),
        STROKE_COLOR("strokeColor", { array ->
            if (array.isPrimitive) {
                setIntValues(*(array as Array<Int>).toIntArray())
            } else {
                setIntValues(*array.filterIsInstance<Int>().toIntArray())
            }
        }),
        STROKE_WIDTH("strokeWidth", { array ->
            if (array.isPrimitive) {
                setFloatValues(*(array as Array<Float>).toFloatArray())
            } else {
                setFloatValues(*array.filterIsInstance<Float>().toFloatArray())
            }
        }),
        STROKE_ALPHA("strokeAlpha", { array ->
            if (array.isPrimitive) {
                setIntValues(*(array as Array<Int>).toIntArray())
            } else {
                setIntValues(*array.filterIsInstance<Int>().toIntArray())
            }
        });

        fun setValues(pvh: PropertyValuesHolder, vararg values: Any) = valueSetter(pvh, values)
    }
}

private inline val Array<*>?.isPrimitive: Boolean get() = this?.firstOrNull()?.javaClass?.isPrimitive == true