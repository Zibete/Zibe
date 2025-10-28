package com.zibete.proyecto1.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

public class ResizableImageViewProfile extends AppCompatImageView {

    public ResizableImageViewProfile(Context context) {
        super(context);
        init();
    }

    public ResizableImageViewProfile(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ResizableImageViewProfile(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Le decimos que mantenga proporciones automáticamente
        setAdjustViewBounds(true);
        setScaleType(ScaleType.FIT_CENTER);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Drawable drawable = getDrawable();

        if (drawable == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height;

        // Mantener relación de aspecto (ancho / alto)
        float imageRatio = (float) drawable.getIntrinsicWidth() / (float) drawable.getIntrinsicHeight();

        if (width > 0) {
            height = (int) (width / imageRatio);
        } else {
            height = drawable.getIntrinsicHeight();
        }

        setMeasuredDimension(width, height);
    }
}
