package com.suenara.enhancedvectordrawable.internal.animatorparser

import android.animation.ArgbEvaluator
import android.animation.Keyframe
import android.animation.PropertyValuesHolder
import android.animation.TypeEvaluator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.XmlResourceParser
import android.util.TypedValue
import android.view.InflateException
import androidx.core.graphics.PathParser
import androidx.core.graphics.PathParser.PathDataNode
import com.suenara.enhancedvectordrawable.internal.attributeIndices
import com.suenara.enhancedvectordrawable.internal.describe
import com.suenara.enhancedvectordrawable.internal.getTypedValue
import com.suenara.enhancedvectordrawable.internal.parseColorInt
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

internal class PropertyValuesHolderParser(private val context: Context) {
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    @SuppressLint("Recycle")
    fun read(parser: XmlResourceParser, valueType: AnimatorValue<*>): PropertyValuesHolder? {
        val propertyName = AnimatorAttributeGetter.PropertyName.get(context, parser)
        val from = AnimatorAttributeGetter.ValueFrom.get(context, parser)
        val to = AnimatorAttributeGetter.ValueTo.get(context, parser)

        val getFloats = valueType is AnimatorValue.FloatNumber
        var returnValue: PropertyValuesHolder? = null

        if (valueType is AnimatorValue.Path) {
            val nodesFrom = getPathDataNodes(from)
            val nodesTo = getPathDataNodes(to)

            if (nodesFrom != null || nodesTo != null) {
                if (nodesFrom != null) {
                    val evaluator = PathDataEvaluator()
                    if (nodesTo != null) {
                        if (!PathParser.canMorph(nodesFrom, nodesTo)) {
                            throw InflateException("Can't morph from ${(from as? AnimatorValue.Path)?.value} to ${(to as? AnimatorValue.Path)?.value}")
                        }
                        returnValue = PropertyValuesHolder.ofObject(propertyName, evaluator, nodesFrom, nodesTo)
                    } else {
                        returnValue = PropertyValuesHolder.ofObject(propertyName, evaluator, (nodesFrom as Object))
                    }
                } else if (nodesTo != null) {
                    val evaluator = PathDataEvaluator()
                    returnValue = PropertyValuesHolder.ofObject(propertyName, evaluator, (nodesTo as Object))
                }
            }
        } else {
            var evaluator: TypeEvaluator<*>? = null

            if (valueType is AnimatorValue.Color) {
                evaluator = ArgbEvaluator()
            }
            if (getFloats) {
                var valueFrom = 0f
                val valueTo: Float

                if (from != null) {
                    valueFrom = (from as AnimatorValue.FloatNumber).value
                    if (to != null) {
                        valueTo = (to as AnimatorValue.FloatNumber).value
                        returnValue = PropertyValuesHolder.ofFloat(propertyName, valueFrom, valueTo)
                    } else {
                        returnValue = PropertyValuesHolder.ofFloat(propertyName, valueFrom)
                    }
                } else {
                    valueTo = (to as AnimatorValue.FloatNumber).value
                    returnValue = PropertyValuesHolder.ofFloat(propertyName, valueFrom, valueTo)
                }
            } else {
                val valueFrom: Int
                val valueTo: Int
                if (from != null) {
                    valueFrom = if (from is AnimatorValue.Color) {
                        from.value
                    } else {
                        (from as AnimatorValue.IntNumber).value
                    }
                    if (to != null) {
                        valueTo = if (to is AnimatorValue.Color) {
                            to.value
                        } else {
                            (to as AnimatorValue.IntNumber).value
                        }
                        returnValue = PropertyValuesHolder.ofInt(propertyName, valueFrom, valueTo)
                    } else {
                        returnValue = PropertyValuesHolder.ofInt(propertyName, valueFrom)
                    }
                } else {
                    if (to != null) {
                        valueTo = if (to is AnimatorValue.Color) {
                            to.value
                        } else {
                            (to as AnimatorValue.IntNumber).value
                        }
                        returnValue = PropertyValuesHolder.ofInt(propertyName, valueTo)
                    }
                }
            }
            if (returnValue != null && evaluator != null) {
                returnValue.setEvaluator(evaluator)
            }
        }

        return returnValue
    }

    fun readKeyframes(parser: XmlResourceParser, valueType: AnimatorValue<*>?): PropertyValuesHolder? {
        val propertyName = AnimatorAttributeGetter.PropertyName.get(context, parser).takeIf { it.isNotEmpty() }
        var propertyValuesHolder: PropertyValuesHolder? = null
        var actualValueType = valueType ?: AnimatorValue.Undefined
        var event: Int = parser.next()
        val keyframes = mutableListOf<Keyframe>()
        while (event != XmlPullParser.END_TAG && event != XmlPullParser.END_DOCUMENT) {
            val name = parser.name
            if (name.equals(KEYFRAME_TAG)) {
                if (actualValueType == AnimatorValue.Undefined) {
                    actualValueType = inferValueTypeOfKeyframe(parser)
                }
                val keyframe = loadKeyframe(parser, actualValueType)
                keyframes.add(keyframe)
                parser.next()
            }
            event = parser.next()
        }

        var count = keyframes.size
        if (count > 0) {
            val firstKeyframe = keyframes[0]
            val lastKeyframe = keyframes[count - 1]
            val endFraction = lastKeyframe.fraction
            if (endFraction < 1) {
                if (endFraction < 0) {
                    lastKeyframe.setFraction(1f)
                } else {
                    keyframes.add(keyframes.size, createNewKeyframe(lastKeyframe, 1f))
                    ++count
                }
            }
            val startFraction = firstKeyframe.fraction
            if (startFraction != 0f) {
                if (startFraction < 0) {
                    firstKeyframe.fraction = 0f
                } else {
                    keyframes.add(0, createNewKeyframe(firstKeyframe, 0f))
                    ++count;
                }
            }
            val keyframeArray = keyframes.toTypedArray()
            for (i in 0 until count) {
                val keyframe = keyframeArray[i]
                if (keyframe.fraction < 0f) {
                    when (i) {
                        0 -> keyframe.fraction = 0f
                        count - 1 -> keyframe.fraction = 1f
                        else -> {
                            val startIndex = i
                            var endIndex = i
                            for (j in (startIndex + 1) until count - 1) {
                                if (keyframeArray[j].fraction >= 0f) {
                                    break
                                }
                                endIndex = j
                            }
                            val gap = keyframeArray[endIndex + 1].fraction - keyframeArray[startIndex - 1].fraction
                            distributeKeyframes(keyframeArray, gap, startIndex, endIndex)
                        }
                    }
                }
            }
            propertyValuesHolder = PropertyValuesHolder.ofKeyframe(propertyName, *keyframeArray).apply {
                if (actualValueType is AnimatorValue.Color) {
                    setEvaluator(ArgbEvaluator())
                }
            }
        }
        return propertyValuesHolder
    }

    private fun getPathDataNodes(value: AnimatorValue<*>?): Array<PathDataNode>? {
        return (value as? AnimatorValue.Path)?.value?.let(PathParser::createNodesFromPathData)
    }

    private fun inferValueTypeOfKeyframe(parser: XmlResourceParser): AnimatorValue<*> {
        val valueIndex = parser.attributeIndices()["value"]
        return if (valueIndex != null && isColorType(parser, valueIndex)) {
            AnimatorValue.Color(0)
        } else {
            AnimatorValue.FloatNumber(0f)
        }
    }

    private fun isColorType(parser: XmlResourceParser, index: Int): Boolean {
        val tv = parser.getTypedValue(index, context)
        return if (
            (TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT).contains(tv?.type)
        ) {
            true
        } else {
            parser.getAttributeValue(index).getOrNull(0) == '#'
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun loadKeyframe(
        parser: XmlResourceParser,
        valueType: AnimatorValue<*>
    ): Keyframe {
        val keyframe: Keyframe
        val fraction = parser.attributeIndices()["fraction"]?.let { parser.getAttributeFloatValue(it, -1f) } ?: -1f
        val valueIndex = parser.attributeIndices()["value"]
        val hasValue = valueIndex != null
        require(valueIndex != null)

        if (hasValue) {
            val tv = parser.getTypedValue(valueIndex, context)
            keyframe = when (valueType) {
                is AnimatorValue.FloatNumber -> {
                    Keyframe.ofFloat(fraction, tv?.float ?: parser.getAttributeValue(valueIndex).toFloat())
                }
                is AnimatorValue.Color, is AnimatorValue.IntNumber -> {
                    val value = parser.getAttributeValue(valueIndex).let { it.toIntOrNull() ?: it.parseColorInt() }
                    Keyframe.ofInt(fraction, value)
                }
                else -> throw IllegalStateException("unreachable ${parser.describe(valueIndex)}")
            }
        } else {
            keyframe = if (valueType is AnimatorValue.FloatNumber) {
                Keyframe.ofFloat(fraction)
            } else {
                Keyframe.ofInt(fraction)
            }
        }

        parser.attributeIndices()["interpolator"]?.run {
            AnimatorAttributeGetter.Interpolator.get(context, parser)
        }?.let {
            keyframe?.interpolator = it
        }
        return keyframe
    }

    private fun createNewKeyframe(sampleKeyframe: Keyframe, fraction: Float): Keyframe = when (sampleKeyframe.type) {
        Float::class.java -> Keyframe.ofFloat(fraction)
        Int::class.java -> Keyframe.ofInt(fraction)
        else -> Keyframe.ofObject(fraction)
    }

    private fun distributeKeyframes(keyframes: Array<Keyframe>, gap: Float, startIndex: Int, endIndex: Int) {
        val count = endIndex - startIndex + 2
        val increment = gap / count
        for (i in startIndex..endIndex) {
            keyframes[i].fraction = keyframes[i - 1].fraction + increment
        }
    }

    private class PathDataEvaluator
    @JvmOverloads constructor(private var nodeArray: Array<PathDataNode>? = null) : TypeEvaluator<Array<PathDataNode>> {


        override fun evaluate(
            fraction: Float,
            startValue: Array<PathDataNode>?,
            endValue: Array<PathDataNode>?
        ): Array<PathDataNode> {
            if (!PathParser.canMorph(startValue, endValue)) {
                throw IllegalArgumentException("Can't interpolate between two incompatible pathData")
            }

            if (!PathParser.canMorph(nodeArray, startValue)) {
                nodeArray = startValue?.let(PathParser::deepCopyNodes)
            }
            val nodeArray = requireNotNull(nodeArray)
            requireNotNull(startValue)
            for (i in startValue.indices) {
                if (endValue != null) {
                    nodeArray[i].interpolatePathDataNode(startValue[i], endValue[i], fraction)
                }
            }

            return nodeArray
        }
    }

    companion object {
        private const val KEYFRAME_TAG = "keyframe"
    }
}