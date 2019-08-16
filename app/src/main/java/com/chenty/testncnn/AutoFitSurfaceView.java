/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chenty.testncnn;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.nfc.Tag;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

/**
 * A {@link TextureView} that can be adjusted to a specified aspect ratio.
 */
public class AutoFitSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "AutoFitSurfaceView";

    private SurfaceHolder mHolder;
    private Canvas mCanvas;
    private Paint paint;
    private Bitmap mBitmap;
    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType, rgbaType;
    private Allocation in, out;

    private float mWidth = 0;
    private float mHeight = 0;

    public AutoFitSurfaceView(Context context) {
        this(context, null);
    }

    public AutoFitSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Log.i(TAG, "AutoFitSurfaceView struct");
        mHolder = getHolder();
        mHolder.addCallback(this);
        paint = new Paint();
        paint.setColor(Color.WHITE);
        setFocusable(true);
        rs = RenderScript.create(context);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
    }

    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        Log.i(TAG, "setAspectRatio " + width + "X" + height);
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "surfaceCreated ");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "surfaceChanged " + width + "X" + height);
        mWidth = width;
        mHeight = height;

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "surfaceDestroyed ");
    }

    private  void DrawRect(Rect rect, int color, String string)
    {
        paint.setColor(color) ;
        paint.setStyle(Paint.Style.STROKE);
        paint.setTextSize(24f);
        paint.setStrokeWidth(3.6f);

        mCanvas.drawRect(rect, paint);
        paint.setStyle(Paint.Style.FILL);
        mCanvas.drawText(string, rect.left + 4, rect.top + 28, paint);
    }

    static private int class_color[] = {
            Color.YELLOW,Color.GREEN,Color.BLUE,Color.YELLOW,Color.BLUE,
            Color.YELLOW,Color.BLUE,Color.BLUE,Color.YELLOW,Color.YELLOW,
            Color.YELLOW,Color.YELLOW,Color.YELLOW,Color.YELLOW,Color.YELLOW,
            Color.RED,Color.YELLOW,Color.YELLOW,Color.YELLOW,Color.YELLOW,
            Color.YELLOW,
    };
    static private String class_names[] = {
            "background", "aeroplane", "bicycle", "bird", "boat",
            "bottle", "bus", "car", "cat", "chair",
            "cow", "diningtable", "dog", "horse", "motorbike",
            "person", "pottedplant", "sheep", "sofa", "train", "tvmonitor"
    };

    public void DrawDetect(double[] data, int width, int height, int rolatedeg) {
        int i,objcnt;
        int t,x,y,xe,ye;
        int prob;
        objcnt = 24;
        for (i = 0 ; i < objcnt ; i++)
        {
            if (data[i*6] > 0)
            {
                t = (int) data[i*6];
                prob = (int)(data[i*6 + 1] * (100));
                x = (int) (data[i*6 + 2] * (width));
                y = (int) (data[i*6 + 3] * (height));
                xe = (int) (data[i*6 + 4] * (width));
                ye = (int) (data[i*6 + 5] * (height));

                if(x < 0) {
                    x = 0;
                }
                if(y < 0) {
                    y = 0;
                }
                if(xe > width) {
                    xe = width;
                }
                if(ye > height) {
                    ye = height;
                }
                Log.d(TAG, "obj" + i +" " + t +" " + prob+ " pos" + x + " " + y + " " + xe + " " + ye);
                DrawRect(new Rect(x,y,xe,ye),class_color[t], class_names[t] + " " + prob);
            }

        }
    }

    public void Draw(byte[] data, int width, int height, double[] result, int rolatedeg) {
        Log.d(TAG, "draw "+data.length +" " + width + "X" + height);
        if (data != null) {

            mBitmap = yuvToBitmap(data,width,height);
            if (mBitmap == null) {
                Log.d(TAG, "data data is to bitmap error");
                return;
            }

            try {
                mCanvas = mHolder.lockCanvas();
                mWidth = mCanvas.getWidth();
                mHeight = mCanvas.getHeight();
                Log.d(TAG, "canvas size is " +  mWidth+ " " + mHeight);
                float scaleWidth = mWidth/width;
                float scaleHeight = mHeight/height;
                Log.d(TAG, "scale size is " +  scaleWidth+ " " +  scaleHeight);


                Matrix matrix = new Matrix();
                matrix.setScale(scaleWidth, scaleHeight);
                mCanvas.drawBitmap(mBitmap, matrix, paint);

                DrawDetect(result, (int)mWidth, (int)mHeight, rolatedeg);
            }catch (Exception e){
                Log.d(TAG, "e="+e);
                mHolder.unlockCanvasAndPost(mCanvas);
                return;
            }
            mHolder.unlockCanvasAndPost(mCanvas);
        } else {
            Log.d(TAG, "data data is null");
            return;
        }
    }

    public Bitmap yuvToBitmap(byte[] yuv, int width, int height){
        if (yuvType == null){
            yuvType = new Type.Builder(rs, Element.U8(rs)).setX(yuv.length);
            in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
            rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(width).setY(height);
            out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
        }
        in.copyFrom(yuv);
        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);
        Bitmap rgbout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        out.copyTo(rgbout);
        return rgbout;
    }
}
