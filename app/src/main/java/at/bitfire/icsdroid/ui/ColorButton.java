package at.bitfire.icsdroid.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
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
