package com.wosloveslife.tipview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by leonard on 17/7/20.
 */

public class TutorialLayout extends FrameLayout {
    public final ShowcaseView mShowcaseView;

    private List<CableView> mCables = new ArrayList<>();

    private boolean mOutsideTouchable;
    private boolean mShowing;

    public static class Builder {
        private final TutorialLayout mTutorialLayout;

        public Builder(Context context) {
            mTutorialLayout = new TutorialLayout(context);
        }

        public Builder addView(View view, FrameLayout.LayoutParams params) {
            mTutorialLayout.addView(view, params);
            return this;
        }

        public Builder addCable(View anchor, View target) {
            CableView cable = new CableView(anchor.getContext(), anchor.getResources().getColor(android.R.color.white), (int) (anchor.getContext().getResources().getDisplayMetrics().density * 6));
            cable.setAnchor(new CableView.Note(anchor, Gravity.NO_GRAVITY, cable.getDefaultLocationChangedListener()));
            cable.setTarget(new CableView.Note(target, Gravity.NO_GRAVITY, cable.getDefaultLocationChangedListener()));

            mTutorialLayout.addCable(cable);
            return this;
        }

        public Builder addExhibit(ShowcaseView.Exhibit exhibit) {
            mTutorialLayout.mShowcaseView.addExhibit(exhibit);
            return this;
        }

        public Builder addExhibit(@ShowcaseView.Exhibit.Shape int shape, final boolean perfect, @NonNull final View anchor, boolean touchable) {
            mTutorialLayout.mShowcaseView.addExhibit(new ShowcaseView.Exhibit(shape, perfect, anchor, mTutorialLayout.mShowcaseView.getDefaultLocationChangedListener()));
            if (touchable) {
                mTutorialLayout.addTouchableView(anchor);
            }
            return this;
        }

        public Builder addClickableRange(View anchor) {
            mTutorialLayout.addTouchableView(anchor);
            return this;
        }

        public Builder setOutsideTouchable(boolean touchable) {
            mTutorialLayout.setOutsideTouchable(touchable);
            return this;
        }

        public TutorialLayout build() {
            return mTutorialLayout;
        }
    }

    public TutorialLayout(@NonNull Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        mShowcaseView = new ShowcaseView(getContext());
        addView(mShowcaseView);

        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOutsideTouchable) {
                    dismiss();
                }
            }
        });
    }

    public void addCable(CableView cable) {
        if (cable != null) {
            mCables.add(cable);
            addView(cable);
        }
    }

    public boolean removeCable(CableView cable) {
        removeView(cable);
        return mCables.remove(cable);
    }

    public void addTouchableView(View view) {
        if (view != null) {
            mTouchableViews.add(view);
        }
    }

    public boolean removeTouchableView(View view) {
        return mTouchableViews.remove(view);
    }

    public void setOutsideTouchable(boolean touchable) {
        mOutsideTouchable = touchable;
    }

    public static boolean containStatusBar(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if ((WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS & activity.getWindow().getAttributes().flags) == WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS) {
                return true;
            }
        }
        return false;
    }

    private void anim(boolean show, Animator.AnimatorListener listener) {
        ValueAnimator animator = ValueAnimator.ofFloat(getAlpha(), show ? 1 : 0);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Float value = (Float) animation.getAnimatedValue();
                setAlpha(value);
            }
        });
        animator.addListener(listener);
        animator.setDuration(160);
        animator.start();
    }

    public void dismiss() {
        if (!mShowing) {
            return;
        }
        anim(false, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                destroy();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                destroy();
            }
        });
    }

    private void destroy() {
        ViewParent parent = getParent();
        if (parent instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) parent;
            viewGroup.removeView(TutorialLayout.this);
        }
        mShowing = false;
    }

    public void show(Activity activity) {
        if (getParent() != null) {
            return;
        }
        ViewGroup container = (ViewGroup) activity.getWindow().getDecorView().findViewById(android.R.id.content);
        container.addView(this);
        mShowing = true;
    }

    private List<View> mTouchableViews = new ArrayList<>();

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        float x = ev.getRawX();
        float y = ev.getRawY();
        Rect rect = new Rect();
        for (View view : mTouchableViews) {
            view.getGlobalVisibleRect(rect);
            if (rect.contains((int) x, (int) y)) {
                return false;
            }
        }

        return !mOutsideTouchable || super.onTouchEvent(ev);
    }

    public boolean isShowing() {
        return mShowing;
    }

    @Override
    public boolean isShown() {
        return isShowing();
    }
}
