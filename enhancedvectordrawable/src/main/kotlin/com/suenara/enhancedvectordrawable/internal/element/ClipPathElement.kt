package com.suenara.enhancedvectordrawable.internal.element

import android.graphics.*
import androidx.annotation.Keep
import androidx.core.graphics.PathParser
import com.suenara.enhancedvectordrawable.AnimationTarget

@Keep
internal class ClipPathElement(
    val name: String?,
    pathData: String?
) : AnimationTarget {
    val path by lazy { Path(originalPath) }

    private val originalPath = pathData?.let(PathParser::createPathFromPathData) ?: Path()
    private val clipPaint = Paint().apply {
        isAntiAlias = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    constructor(prototype: ClipPathElement) : this(prototype.name, null) {
        originalPath.set(prototype.originalPath)
        path.set(prototype.path)
        clipPaint.set(prototype.clipPaint)
    }

    fun transform(matrix: Matrix) {
        path.set(originalPath)
        path.transform(matrix)
    }

}