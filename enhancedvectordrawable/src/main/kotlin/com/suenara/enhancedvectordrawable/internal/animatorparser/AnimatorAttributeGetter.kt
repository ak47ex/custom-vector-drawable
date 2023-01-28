package com.suenara.enhancedvectordrawable.internal.animatorparser

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.XmlResourceParser
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import com.suenara.enhancedvectordrawable.internal.attributeIndices
import com.suenara.enhancedvectordrawable.internal.dimensionValue
import com.suenara.enhancedvectordrawable.internal.parseColorInt

internal sealed class AnimatorAttributeGetter<T>(
    protected val attribute: AnimatorAttribute,
    private val defaultValue: T
) {

    abstract fun convert(context: Context, parser: XmlResourceParser, index: Int): T

    fun get(context: Context, parser: XmlResourceParser): T {
        val index = parser.attributeIndices()[attribute.tag] ?: return defaultValue
        return convert(context, parser, index)
    }

    object Interpolator : AnimatorAttributeGetter<android.view.animation.Interpolator>(
        AnimatorAttribute.INTERPOLATOR,
        AccelerateDecelerateInterpolator()
    ) {
        override fun convert(
            context: Context,
            parser: XmlResourceParser,
            index: Int
        ): android.view.animation.Interpolator {
            val interpolatorRes = parser.getAttributeResourceValue(index, 0)
            return if (interpolatorRes != 0) {
                AnimationUtils.loadInterpolator(context, interpolatorRes)
            } else {
                throw IllegalStateException("Can't parse interpolator")
            }
        }
    }

    object Duration : AnimatorAttributeGetter<Long>(AnimatorAttribute.DURATION, 300L) {
        override fun convert(context: Context, parser: XmlResourceParser, index: Int): Long =
            parser.getAttributeValue(index).toLongOrNull() ?: 0L
    }

    sealed class Value(attribute: AnimatorAttribute) :
        AnimatorAttributeGetter<AnimatorValue<*>?>(attribute, null) {
        override fun convert(context: Context, parser: XmlResourceParser, index: Int): AnimatorValue<*> {
            val valueType = ValueType.get(context, parser).let {
                if (it !is AnimatorValue.Color  && parser.getAttributeValue(index).startsWith('#')) {
                    AnimatorValue.Color(0)
                } else {
                    it
                }
            }
            return when (valueType) {
                is AnimatorValue.Color -> AnimatorValue.Color(parser.getAttributeValue(index).parseColorInt())
                is AnimatorValue.FloatNumber -> AnimatorValue.FloatNumber(parser.floatAt(index, context))
                is AnimatorValue.IntNumber -> AnimatorValue.IntNumber(parser.intAt(index))
                is AnimatorValue.Path -> AnimatorValue.Path(parser.getAttributeValue(index))
                AnimatorValue.Undefined -> throw IllegalStateException("Undefined ${attribute.tag} type")
            }
        }
    }

    object ValueFrom : Value(AnimatorAttribute.VALUE_FROM)
    object ValueTo : Value(AnimatorAttribute.VALUE_TO)


    object ValueType : AnimatorAttributeGetter<AnimatorValue<*>>(
        AnimatorAttribute.VALUE_TYPE,
        AnimatorValue.FloatNumber(0f)
    ) {
        override fun convert(context: Context, parser: XmlResourceParser, index: Int): AnimatorValue<*> {
            val value = listOf(AnimatorAttribute.VALUE_FROM, AnimatorAttribute.VALUE_TO).firstNotNullOfOrNull {
                parser.attributeIndices()[it.tag]
            }?.let {
                parser.getAttributeValue(it)
            }

            val valueType = if (value?.startsWith('#') == true) 3 else parser.intAt(index)
            return when (valueType) {
                0 -> AnimatorValue.FloatNumber(0f)
                1 -> AnimatorValue.IntNumber(0)
                2 -> AnimatorValue.Path("")
                3 -> AnimatorValue.Color(0)
                4 -> AnimatorValue.Undefined
                else -> throw IllegalStateException("unknown value type ${parser.getAttributeValue(index)}")
            }
        }
    }

    object PropertyName : AnimatorAttributeGetter<String>(AnimatorAttribute.PROPERTY_NAME, "") {
        override fun convert(context: Context, parser: XmlResourceParser, index: Int): String =
            parser.getAttributeValue(index)
    }

    object PropertyXName : AnimatorAttributeGetter<String>(AnimatorAttribute.PROPERTY_X_NAME, "") {
        override fun convert(context: Context, parser: XmlResourceParser, index: Int): String =
            parser.getAttributeValue(index)
    }

    object PropertyYName : AnimatorAttributeGetter<String>(AnimatorAttribute.PROPERTY_Y_NAME, "") {
        override fun convert(context: Context, parser: XmlResourceParser, index: Int): String =
            parser.getAttributeValue(index)
    }

    object PathData : AnimatorAttributeGetter<String>(AnimatorAttribute.PATH_DATA, "") {
        override fun convert(context: Context, parser: XmlResourceParser, index: Int): String =
            parser.getAttributeValue(index)
    }


    object StartDelay : AnimatorAttributeGetter<Long>(AnimatorAttribute.START_OFFSET, 0) {
        override fun convert(context: Context, parser: XmlResourceParser, index: Int): Long = parser.longAt(index)
    }

    object RepeatCount : AnimatorAttributeGetter<Int>(AnimatorAttribute.REPEAT_COUNT, 0) {
        override fun convert(context: Context, parser: XmlResourceParser, index: Int): Int = parser.intAt(index)
    }

    object RepeatMode : AnimatorAttributeGetter<Int>(AnimatorAttribute.REPEAT_MODE, ValueAnimator.RESTART) {
        override fun convert(context: Context, parser: XmlResourceParser, index: Int): Int = parser.intAt(index)
    }

    protected fun XmlResourceParser.intAt(index: Int): Int = getAttributeValue(index).toInt()
    protected fun XmlResourceParser.longAt(index: Int): Long = getAttributeValue(index).toLong()
    protected fun XmlResourceParser.floatAt(index: Int, context: Context): Float =
        dimensionValue(context, getAttributeValue(index))

    protected enum class AnimatorAttribute(val tag: String) {
        INTERPOLATOR("interpolator"),
        DURATION("duration"),
        VALUE_FROM("valueFrom"),
        VALUE_TO("valueTo"),
        VALUE_TYPE("valueType"),
        PROPERTY_NAME("propertyName"),
        PROPERTY_X_NAME("propertyXName"),
        PROPERTY_Y_NAME("propertyYName"),
        PATH_DATA("pathData"),
        START_OFFSET("startOffset"),
        REPEAT_COUNT("repeatCount"),
        REPEAT_MODE("repeatMode")
    }
}