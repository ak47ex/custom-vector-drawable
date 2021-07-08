package com.suenara.animatedcustomvectordrawable

import android.animation.*
import android.content.Context
import android.content.res.Resources
import android.content.res.XmlResourceParser
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.Log
import android.util.Xml
import android.view.InflateException
import androidx.core.graphics.PathParser
import androidx.vectordrawable.graphics.drawable.AnimatorInflaterCompat
import com.suenara.customvectordrawable.CachedParser
import com.suenara.customvectordrawable.attributeIndices
import org.xmlpull.v1.XmlPullParser

class AnimatorParser(private val context: Context) {
    private val resources: Resources = context.resources

    fun read(resId: Int): Animator {
        val parser = resources.getAnimation(resId)
        return parseAnimator(parser)!!
    }

    private fun parseAnimator(
        parser: XmlResourceParser,
        parent: AnimatorSet? = null,
        sequenceOrdering: Int = 0
    ): Animator? {
        var event = parser.eventType
        val depth = parser.depth
        var childAnims: MutableList<Animator>? = null
        var anim: Animator? = null
        var gotValues = false
        parser.next()
        while ((event != XmlPullParser.END_TAG || parser.depth > depth) && event != XmlPullParser.END_DOCUMENT) {
            if (event != XmlPullParser.START_TAG) {
                event = parser.next()
                continue
            }

            val name = parser.name
            when (name) {
                OBJECT_ANIMATOR -> {
                    anim = readObjectAnimator(CachedParser(parser))
                }
                ANIMATOR -> {
                    anim = readAnimator(parser)
                }
                SET -> {
                    anim = readAnimatorSet(parser)
                    val ordering = parser.attributeIndices()[ORDERING]?.let { index ->
                        parser.getAttributeValue(index).toIntOrNull()
                    } ?: TOGETHER
                    parseAnimator(parser, anim, ordering)
                }
                PROPERTY_VALUES_HOLDER -> {
                    readPropertyValuesHolder(parser)
                    gotValues = true
                }
            }

            if (parent != null && !gotValues) {
                if (childAnims == null) {
                    childAnims = ArrayList()
                }
                anim?.let { childAnims.add(it) }
            }
            event = parser.next()
        }
        if (parent != null && childAnims != null) {
            if (sequenceOrdering == TOGETHER) {
                parent.playTogether(*childAnims.toTypedArray())
            } else {
                parent.playSequentially(*childAnims.toTypedArray())
            }
        }
        return anim
    }

    private fun readObjectAnimator(parser: XmlResourceParser): ObjectAnimator {
        val animator = ObjectAnimator()
        animator.interpolator = AnimatorAttribute.Interpolator.get(context, parser)
        animator.duration = AnimatorAttribute.Duration.get(context, parser)
        animator.startDelay = AnimatorAttribute.StartDelay.get(context, parser)
        animator.repeatCount = AnimatorAttribute.RepeatCount.get(context, parser)
        animator.repeatMode = AnimatorAttribute.RepeatMode.get(context, parser)
        getPropertyValuesHolder(parser)?.let {
            animator.setValues(it)
        }
        setupObjectAnimator(parser, animator)

        return animator
    }

    private fun setupObjectAnimator(parser: XmlResourceParser, animator: ObjectAnimator, pixelSize: Float = 1f) {
        val pathData = AnimatorAttribute.PathData.get(context, parser)
        if (pathData.isNotEmpty()) {
            val propertyXName = AnimatorAttribute.PropertyXName.get(context, parser)
            val propertyYName = AnimatorAttribute.PropertyYName.get(context, parser)

            var valueType = AnimatorAttribute.ValueType.get(context, parser)
            if (valueType is AnimatorValue.Path || valueType is AnimatorValue.Undefined) {
                valueType = AnimatorValue.FloatNumber(0f)
            }
            if (propertyXName.isEmpty() && propertyXName.isEmpty()) {
                throw InflateException("propertyXName or propertyYName is need for PathData")
            } else {
                val path = PathParser.createPathFromPathData(pathData)
                setupPathMotion(path, animator, 0.5f * pixelSize, propertyXName, propertyYName)
            }
        } else {
            animator.setPropertyName(AnimatorAttribute.PropertyName.get(context, parser))
        }
    }

    private fun setupPathMotion(
        path: Path,
        oa: ObjectAnimator,
        precision: Float,
        propertyXName: String,
        propertyYName: String
    ) {
        val measureForTotalLength = PathMeasure(path, false)
        var totalLength = 0f
        val contourLengths = ArrayList<Float>()
        contourLengths.add(0f)
        do {
            val pathLength = measureForTotalLength.length
            totalLength += pathLength
            contourLengths.add(totalLength)
        } while (measureForTotalLength.nextContour())


        val pathMeasure = PathMeasure(path, false)
        val numPoints = minOf(MAX_NUM_POINTS, (totalLength / precision).toInt() + 1)
        val mX = FloatArray(numPoints)
        val mY = FloatArray(numPoints)
        val position = FloatArray(2)

        var contourIndex = 0
        val step = totalLength / (numPoints - 1)
        var currentDistance = 0f

        for (i in 0 until numPoints) {
            pathMeasure.getPosTan(currentDistance - contourLengths[contourIndex], position, null)
            mX[i] = position[0]
            mY[i] = position[1]
            currentDistance += step
            if ((contourIndex + 1) < contourLengths.size && currentDistance > contourLengths[contourIndex + 1]) {
                contourIndex++
                pathMeasure.nextContour()
            }
        }

        val x = propertyXName.takeIf { it.isNotEmpty() }?.let { PropertyValuesHolder.ofFloat(it, *mX) }
        val y = propertyYName.takeIf { it.isNotEmpty() }?.let { PropertyValuesHolder.ofFloat(it, *mY) }

        when {
            x == null -> oa.setValues(y)
            y == null -> oa.setValues(x)
            else -> oa.setValues(x, y)
        }
    }

    private fun readAnimator(parser: XmlResourceParser): Animator = TODO()
    private fun readAnimatorSet(parser: XmlResourceParser): AnimatorSet = AnimatorSet()
    private fun readPropertyValuesHolder(parser: XmlResourceParser): PropertyValuesHolder = TODO()

    private fun readInterpolator(parser: XmlResourceParser) {

    }

    private fun getPropertyValuesHolder(parser: XmlResourceParser): PropertyValuesHolder? {
        return PropertyValuesHolderParser(context).read(parser)
    }

    companion object {
        private const val OBJECT_ANIMATOR = "objectAnimator"
        private const val ANIMATOR = "animator"
        private const val SET = "set"
        private const val PROPERTY_VALUES_HOLDER = "propertyValuesHolder"
        private const val TOGETHER = 0
        private const val SEQUENTIALLY = 1
        private const val MAX_NUM_POINTS = 100
        private const val ORDERING = "ordering"

        private const val PATH_DATA = "pathData"
    }
}