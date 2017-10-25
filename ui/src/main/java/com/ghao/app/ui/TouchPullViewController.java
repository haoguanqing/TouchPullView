package com.ghao.app.ui;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;

/**
 * Created by ghao on 10/22/17. ( ´∀｀)σ
 */

public class TouchPullViewController implements View.OnTouchListener {
    private static final float TOUCH_MOVE_MAX_Y = 600;
    private float mDownY;
    private boolean mIsPulling = false;

    private ViewGroup mParent;
    @NotNull private TouchPullView mTouchPullView;


    public TouchPullViewController(@NotNull ViewGroup parent) {
        mParent = parent.findViewById(R.id.touch_pull_container);
        mTouchPullView = parent.findViewById(R.id.touch_pull_view);
    }

    public void init() {
        mParent.setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(final View view, final MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownY = event.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                final float y = event.getY();
                if (y >= mDownY) {
                    mIsPulling = true;
                    float progress = y - mDownY > TOUCH_MOVE_MAX_Y ?
                            1 : (y - mDownY) / TOUCH_MOVE_MAX_Y;
                    mTouchPullView.setProgress(progress);
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (mIsPulling) {
                    mTouchPullView.release();
                    mIsPulling = false;
                    return true;
                }
                return false;
            default:
                mIsPulling = false;
                return false;
        }
    }
}
