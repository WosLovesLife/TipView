package com.wosloveslife.tipview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.ArrayList;
import java.util.List;

import static com.wosloveslife.tipview.ShowcaseView.Exhibit.Shape.SHAPE_OVAL;
import static com.wosloveslife.tipview.ShowcaseView.Exhibit.Shape.SHAPE_RECT;

/**
 * Created by leonard on 17/7/20.
 */

public class ShowcaseView extends View {
    private int mBgColor;
    private final Paint mPaint;
    private Bitmap mBitmapBuffer;
    private Canvas mBufferCanvas;
    private PorterDuffXfermode mXfermode;

    private int mStatusBarOffset;

    public static class Exhibit {

        @IntDef({SHAPE_RECT, SHAPE_OVAL})
        public @interface Shape {
            int SHAPE_RECT = 0;
            int SHAPE_OVAL = 2;
        }

        @Shape
        int mShape;
        RectF mRectF;

        @Nullable
        private View mAnchor;
        private boolean mPerfect;

        public Exhibit(@Shape int shape, int left, int top, int right, int bottom) {
            this(shape, new RectF(left, top, right, bottom));
        }

        public Exhibit(@Shape int shape, RectF rectF) {
            mShape = shape;
            mRectF = rectF;
        }

        public Exhibit(@Shape int shape, final boolean perfect, @NonNull final View anchor, final OnLocationChangedListener listener) {
            mShape = shape;
            mAnchor = anchor;
            mPerfect = perfect;

            final int[] loc = getLocation(anchor);
            mRectF = createRectF(loc, anchor, perfect);

            anchor.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    final int[] newLoc = getLocation(anchor);

                    if (loc[0] != newLoc[0] || loc[1] != newLoc[1]) {
                        loc[0] = newLoc[0];
                        loc[1] = newLoc[1];
                        mRectF = createRectF(loc, anchor, perfect);
                        listener.onLocationChanged(mRectF);
                    }
                }
            });
        }

        private static int[] getLocation(View anchor) {
            final int[] loc = new int[2];
            anchor.getLocationOnScreen(loc);
            return loc;
        }

        private static RectF createRectF(int[] loc, View anchor, boolean perfect) {
            int w = anchor.getMeasuredWidth();
            int h = anchor.getMeasuredHeight();
            int x = loc[0];
            int y = loc[1];
            if (perfect && w != h) {
                int offset = w - h;
                if (offset > 0) {
                    y -= offset / 2f;
                    h = w;
                } else {
                    x += offset / 2f;
                    w = h;
                }
            }
            return new RectF(x, y, x + w, y + h);
        }

        public void updateLocation() {
            if (mAnchor == null) {
                return;
            }
            mRectF = createRectF(getLocation(mAnchor), mAnchor, mPerfect);
        }

        public interface OnLocationChangedListener {
            void onLocationChanged(RectF rectF);
        }
    }

    private List<Exhibit> mExhibits = new ArrayList<>();

    public ShowcaseView(Context context) {
        this(context, null);
    }

    public ShowcaseView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShowcaseView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        if (getContext() instanceof Activity) {
            if (!TutorialLayout.containStatusBar((Activity) getContext())) {
                mStatusBarOffset = (int) (getResources().getDisplayMetrics().density * 26);
            }
        }

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mXfermode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);

        mBgColor = getResources().getColor(R.color.gray_translucent_bg);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mBitmapBuffer = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
        mBufferCanvas = new Canvas(mBitmapBuffer);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mBufferCanvas.save();
        mBitmapBuffer.eraseColor(mBgColor);
        mBufferCanvas.drawColor(mBgColor);

        mPaint.setXfermode(mXfermode);
        for (Exhibit exhibit : mExhibits) {
            RectF rectF = new RectF(exhibit.mRectF);
            rectF.top -= mStatusBarOffset;
            rectF.bottom -= mStatusBarOffset;
            switch (exhibit.mShape) {
                case SHAPE_OVAL:
                    mBufferCanvas.drawOval(rectF, mPaint);
                    break;
                case SHAPE_RECT:
                    mBufferCanvas.drawRect(rectF, mPaint);
                    break;
            }
        }
        mPaint.setXfermode(null);

        mBufferCanvas.restore();

        canvas.save();
        canvas.drawBitmap(mBitmapBuffer, 0, 0, mPaint);
        canvas.restore();
    }

    private Exhibit.OnLocationChangedListener mOnLocationChangedListener = new Exhibit.OnLocationChangedListener() {

        @Override
        public void onLocationChanged(RectF rectF) {
            invalidate();
        }
    };

    public Exhibit.OnLocationChangedListener getDefaultLocationChangedListener() {
        return mOnLocationChangedListener;
    }

    public void addExhibit(Exhibit exhibit) {
        mExhibits.add(exhibit);
        invalidate();
    }

    public void removeExhibit(Exhibit exhibit) {
        mExhibits.remove(exhibit);
        invalidate();
    }

    public void update() {
        for (Exhibit exhibit : mExhibits) {
            exhibit.updateLocation();
        }
        invalidate();
    }
}
