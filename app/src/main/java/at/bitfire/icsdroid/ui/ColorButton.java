/*
 * Copyright (c) 2013 – 2016 Ricki Hirner (bitfire web engineering).
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 *
 */

package at.bitfire.icsdroid.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.view.View;

public class ColorButton extends View {
    private OvalShape shape;
    private Paint paint;

    public ColorButton(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        shape = new OvalShape();
        paint = new Paint();
        paint.setColor(0xffFF0000);
        paint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        shape.resize(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        shape.draw(canvas, paint);
    }


    public void setColor(int color) {
        paint.setColor(0xff000000 | color);
        invalidate();
    }
}
