package com.zibete.proyecto1.ui;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class ResizableImageViewProfile extends androidx.appcompat.widget.AppCompatImageView {


        public ResizableImageViewProfile(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public ResizableImageViewProfile(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            Drawable image = getDrawable();
            if (image == null) {
                super.setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
                return;
            }


            int width;
            int height;


            int imageHeight = image.getIntrinsicHeight(); //altura image
            int imageWidth = image.getIntrinsicWidth(); //ancho image


            int heightSize = MeasureSpec.getSize(heightMeasureSpec);
            int widthSize = MeasureSpec.getSize(widthMeasureSpec);

            int imageRatio = imageWidth / imageHeight;
            int sizeRatio = widthSize / heightSize;

/*
            if (imageHeight<heightSize){


                if (imageWidth<widthSize){

                    if (imageRatio<sizeRatio){

                        height = heightSize;
                        width = imageWidth;

                    }else{

                        height=imageHeight;
                        width=widthSize;
                    }

                }else {
                    width = imageWidth;
                    height = heightSize;
                }

            }else{

                height = imageHeight;
                width=widthSize;

            }

 */




            if (imageHeight > 0) {
                imageRatio = imageWidth / imageHeight;
            }

            if (heightSize > 0) {
                sizeRatio = widthSize / heightSize;
            }




            if (imageRatio >= sizeRatio) {
                height = heightSize;
                width = heightSize * imageHeight / imageWidth;
            } else {

                width = widthSize;
                height = width * imageHeight / imageWidth;

            }



            setMeasuredDimension(width, height);
        }


}






