/*
 * Copyright (c) Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package at.bitfire.icsdroid.ui

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.view.View;

class ColorButton(
        context: Context,
        attributeSet: AttributeSet?
): View(context, attributeSet) {

    private var shape = OvalShape()
    private var paint = Paint()

    init {
        paint.color = 0xFFFF0000.toInt()
        paint.isAntiAlias = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        shape.resize(w.toFloat(), h.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        shape.draw(canvas, paint)
    }


    fun setColor(color: Int) {
        paint.color = 0xFF000000.toInt() or color
        invalidate()
    }

}
