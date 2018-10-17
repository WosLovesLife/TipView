package com.wosloveslife.tipview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathEffect;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.support.annotation.Px;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;


/**
 * Created by leonard on 17/7/21.
 */

public class CableView extends View {
    private final Paint mPaint;
    private final Paint mPaint2;
    private final Paint mBasicPaint;
    private int mStatusBarOffset;

    Note mAnchor;
    Note mTarget;

    private Path mLinePath;

    public static class Note {
        View mView;
        int mGravity;
        /** 根据View和Gravity计算出线的端点 */
        private Point mPoint;
        /** 自动计算出来的断点处于View的方向 */
        private int mAutoGravity;

        Note(View view, int gravity, final OnLocationChangedListener listener) {
            mView = view;
            mGravity = gravity;

            final int[] loc = getLocation(view);

            view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    final int[] newLoc = getLocation(mView);

                    if (loc[0] != newLoc[0] || loc[1] != newLoc[1]) {
                        loc[0] = newLoc[0];
                        loc[1] = newLoc[1];
                        listener.onLocationChanged();
                    }
                }
            });
        }

        private static int[] getLocation(View anchor) {
            final int[] loc = new int[2];
            anchor.getLocationOnScreen(loc);
            return loc;
        }

        public interface OnLocationChangedListener {
            void onLocationChanged();
        }
    }

    private Note.OnLocationChangedListener mOnLocationChangedListener = new Note.OnLocationChangedListener() {

        @Override
        public void onLocationChanged() {
            update();
        }
    };

    public Note.OnLocationChangedListener getDefaultLocationChangedListener() {
        return mOnLocationChangedListener;
    }

    public CableView(Context context, @ColorInt int color, @Px int lineWidth) {
        super(context);

        if (getContext() instanceof Activity) {
            if (!TutorialLayout.containStatusBar((Activity) getContext())) {
                mStatusBarOffset = (int) (getResources().getDisplayMetrics().density * 26);
            }
        }

        mBasicPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mPaint = new Paint(mBasicPaint);
        mPaint.setColor(color);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(lineWidth);

        Path path = new Path();
        int half = lineWidth / 2;
        path.addRoundRect(new RectF(-lineWidth, -half, lineWidth, half), half, half, Path.Direction.CW);
        PathEffect pathEffect = new PathDashPathEffect(path, lineWidth * 3.5f, 0, PathDashPathEffect.Style.ROTATE);
        mPaint.setPathEffect(pathEffect);

        mPaint2 = new Paint(mBasicPaint);
        mPaint2.setStyle(Paint.Style.STROKE);
        mPaint2.setStrokeCap(Paint.Cap.ROUND);
        mPaint2.setStrokeJoin(Paint.Join.ROUND);
        mPaint2.setStrokeWidth(lineWidth);
        mPaint2.setColor(color);

        createBitmap();
    }

    public void setAnchor(Note anchor) {
        mAnchor = anchor;
    }

    public void setTarget(Note target) {
        mTarget = target;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private float currentValue = 0;     // 用于纪录当前的位置,取值范围[0,1]映射Path的整个长度
    private float[] pos = new float[2];                // 当前点的实际位置
    private float[] tan = new float[2];                // 当前点的tangent值,用于计算图片所需旋转的角度
    private Matrix mMatrix = new Matrix();           // 当前点的tangent值,用于计算图片所需旋转的角度
    private Bitmap mBitmap;             // 箭头图片

    private void createBitmap() {
        float density = getResources().getDisplayMetrics().density;
        int offset = (int) (density * 6);
        int width = (int) (density * 25);
        int half = width / 2;
        int height = (int) (density * 16);

        mBitmap = Bitmap.createBitmap(height + offset * 2, width + offset * 2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mBitmap);

        Path path = new Path();
        path.moveTo(offset, offset);
        path.rLineTo(height, half);
        path.rLineTo(-height, half);
        canvas.drawPath(path, mPaint2);
    }

    private void drawArrow(Canvas canvas) {
        PathMeasure measure = new PathMeasure(mLinePath, false);     // 创建 PathMeasure

        currentValue = 1f;  // 计算当前的位置在总长度上的比例[0,1]

        measure.getPosTan(measure.getLength() * currentValue, pos, tan);        // 获取当前位置的坐标以及趋势
        float degrees = (float) (Math.atan2(tan[1], tan[0]) * 180.0 / Math.PI); // 计算图片旋转角度

        mMatrix.reset();                                                        // 重置Matrix
        mMatrix.postRotate(degrees, mBitmap.getWidth() / 2, mBitmap.getHeight() / 2);   // 旋转图片
        mMatrix.postTranslate(pos[0] - mBitmap.getWidth() / 2, pos[1] - mBitmap.getHeight() / 2);   // 将图片绘制中心调整到与当前点重合

        canvas.save();
        canvas.drawBitmap(mBitmap, mMatrix, mBasicPaint);                     // 绘制箭头
        canvas.restore();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mAnchor.mPoint == null) {
            update();
            return;
        }

        canvas.save();

        canvas.drawPath(mLinePath, mPaint);

        canvas.restore();

        drawArrow(canvas);
    }

    private void calculateAnchorPoint() {
        boolean fromLeftBottom2RightTop;
        boolean fromLeftTop2RightBottom;

        int[] aLoc = new int[2];
        mAnchor.mView.getLocationOnScreen(aLoc);
        int[] tLoc = new int[2];
        mTarget.mView.getLocationOnScreen(tLoc);

        // anchor的中心点
        int x = aLoc[0] + mAnchor.mView.getWidth() / 2;
        int y = aLoc[1] + mAnchor.mView.getHeight() / 2;

        // target的中心点
        int tX = tLoc[0] + mTarget.mView.getWidth() / 2;
        int tY = tLoc[1] + mTarget.mView.getHeight() / 2;

        if (mAnchor.mGravity == Gravity.START || mAnchor.mGravity == Gravity.LEFT) {
            fromLeftBottom2RightTop = true;
            fromLeftTop2RightBottom = false;
        } else if (mAnchor.mGravity == Gravity.END || mAnchor.mGravity == Gravity.RIGHT) {
            fromLeftBottom2RightTop = false;
            fromLeftTop2RightBottom = true;
        } else if (mAnchor.mGravity == Gravity.TOP) {
            fromLeftBottom2RightTop = true;
            fromLeftTop2RightBottom = true;
        } else if (mAnchor.mGravity == Gravity.BOTTOM) {
            fromLeftBottom2RightTop = false;
            fromLeftTop2RightBottom = false;
        } else {
            // anchor右上角
            int iX = aLoc[0] + mAnchor.mView.getWidth();
            int iY = aLoc[1];

            // anchor右下角
            int jX = iX;
            int jY = aLoc[1] + mAnchor.mView.getHeight();

            // 计算target中心点是否在iX之上
            float iX1 = iX - x;
            float iY1 = iY - y;
            float jX1 = jX - x;
            float jY1 = jY - y;
            float tX1 = tX - x;
            float tY1 = tY - y;
            float iRate = iY1 / iX1;
            float jRate = jY1 / jX1;
            float tRate = tY1 / tX1;

            fromLeftBottom2RightTop = tX1 > 0 ? iRate > tRate : iRate < tRate;
            fromLeftTop2RightBottom = tX1 > 0 ? jRate > tRate : jRate < tRate;
        }

        if (fromLeftBottom2RightTop) {
            if (fromLeftTop2RightBottom) {
                mAnchor.mPoint = new Point(x, aLoc[1]);
                mAnchor.mAutoGravity = Gravity.TOP;
            } else {
                mAnchor.mPoint = new Point(aLoc[0], y);
                mAnchor.mAutoGravity = Gravity.RIGHT;
            }
        } else {
            if (fromLeftTop2RightBottom) {
                mAnchor.mPoint = new Point(aLoc[0] + mAnchor.mView.getWidth(), y);
                mAnchor.mAutoGravity = Gravity.BOTTOM;
            } else {
                mAnchor.mPoint = new Point(x, aLoc[1] + mAnchor.mView.getHeight());
                mAnchor.mAutoGravity = Gravity.LEFT;
            }
        }
    }

    private void calculateTargetPoint() {
        boolean above;
        boolean leftOf;

        int[] targetLocation = new int[2];
//        int[] anchorLocation = new int[2];
        mTarget.mView.getLocationOnScreen(targetLocation);
//        mAnchor.mView.getLocationOnScreen(anchorLocation);
//        leftOf = targetLocation[0] < anchorLocation[0];
//        above = targetLocation[1] < anchorLocation[1];
//
//        mTarget.mPoint = new Point();
//        mTarget.mPoint.x = targetLocation[0] + (leftOf ? mTarget.mView.getWidth() : 0);
//        mTarget.mPoint.y = targetLocation[1] + (above ? mTarget.mView.getHeight() : 0) - mStatusBarOffset; // TODO: 17/7/25 减去StatusBar高度
        mTarget.mPoint = new Point();
        mTarget.mPoint.x = targetLocation[0] + mTarget.mView.getWidth() / 2;
        mTarget.mPoint.y = targetLocation[1] + mTarget.mView.getHeight() / 2;
    }

    public void update() {
        calculateAnchorPoint();

        calculateTargetPoint();

        mLinePath = new Path();
        int offsetX = (int) ((mTarget.mPoint.x - mAnchor.mPoint.x) * 0.15f);

        int offX = mTarget.mPoint.x - mAnchor.mPoint.x;
        int offY = mTarget.mPoint.y - mAnchor.mPoint.y;

        float offX1 = mAnchor.mPoint.x;
        float offY1 = mAnchor.mPoint.y + offY * 0.66f;
        mLinePath.moveTo(mAnchor.mPoint.x, mAnchor.mPoint.y - mStatusBarOffset);
        mLinePath.quadTo(offX1, offY1 - mStatusBarOffset, mTarget.mPoint.x, mTarget.mPoint.y);

        PathMeasure measure = new PathMeasure(mLinePath, false);
        Path newPath = new Path();
        float length = measure.getLength() - mTarget.mView.getWidth() / 2;
        length -= getResources().getDisplayMetrics().density * 4;
        measure.getSegment(measure.getLength() * 0.07f, length, newPath, true);
        mLinePath = newPath;

        invalidate();
    }
}
