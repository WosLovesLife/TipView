package com.wosloveslife.tipview;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by leonard on 17/7/26.
 */

public class TouchableView extends View {
    private List<View> mTouchableViews = new ArrayList<>();

    public TouchableView(Context context) {
        this(context, null);
    }

    public TouchableView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TouchableView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void addTouchableView(View view) {
        if (view != null) {
            mTouchableViews.add(view);
        }
    }

    public boolean removeTouchableView(View view) {
        return mTouchableViews.remove(view);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        float x = ev.getRawX();
        float y = ev.getRawY();
        Rect rect = new Rect();
        for (View view : mTouchableViews) {
            view.getGlobalVisibleRect(rect);
            if (rect.contains((int) x, (int) y)) {
                Log.w("Leonard", "onTouchEvent: ");
                return false;
            }
        }
        return super.onTouchEvent(ev);
    }
}
