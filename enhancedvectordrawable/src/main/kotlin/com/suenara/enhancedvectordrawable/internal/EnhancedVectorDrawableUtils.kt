package com.suenara.enhancedvectordrawable.internal

import android.content.Context
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue

internal fun floatAlphaToInt(alpha: Float): Int = minOf(255, (255 * alpha).toInt())

//TODO: need correct implementation
internal class CachedParser(val parser: XmlResourceParser) : XmlResourceParser by parser

internal fun AttributeSet.attributeIndices(): Map<String, Int> {
    return mutableMapOf<String, Int>().also { map ->
        for (idx in 0 until attributeCount) map[getAttributeName(idx)] = idx
    }
}

internal fun String.parseColorInt(): Int {
    return when (length) {
        2 -> {
            Color.parseColor(buildString {
                append('#')
                repeat(8) { append(this@parseColorInt[1]) }
            })
        }
        4 -> {
            Color.parseColor(buildString {
                append('#')
                append(this@parseColorInt[1])
                append(this@parseColorInt[1])
                append(this@parseColorInt[2])
                append(this@parseColorInt[2])
                append(this@parseColorInt[3])
                append(this@parseColorInt[3])
            })
        }
        7 -> Color.parseColor(this)
        9 -> Color.parseColor(this)
        else -> Color.TRANSPARENT
    }
}

internal fun dimensionValue(context: Context, value: String): Float {
    return try {
        DimensionConverter.stringToDimension(value, context)
    } catch (_: NumberFormatException) {
        value.toFloat()
    }
}

internal fun XmlResourceParser.describe(index: Int): String {
    return """
        name: ${getAttributeName(index)},
        value: ${getAttributeValue(index)},
        type: ${getAttributeType(index)},
        attribute resource value: ${getAttributeResourceValue(index, 0)}
        attribute name resource: ${getAttributeNameResource(index)}
        attribute namespace: ${getAttributeNamespace(index)}
    """.trimIndent()
}

internal fun XmlResourceParser.getTypedValue(index: Int, context: Context): TypedValue? =
    getTypedValue(index, context.resources)

internal fun XmlResourceParser.getTypedValue(index: Int, resources: Resources): TypedValue? {
    val resId = getAttributeResourceValue(index, 0)
    return if (resId != 0) {
        TypedValue().also {
            resources.getValue(resId, it, true)
        }
    } else {
        null
    }
}