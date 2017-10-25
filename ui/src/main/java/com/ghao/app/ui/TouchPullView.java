package com.ghao.app.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

/**
 * Created by ghao on 10/22/17. ( ´∀｀)σ
 */

public class TouchPullView extends View {
    private static final boolean IS_DEBUG = true;
    private static final Interpolator DECELERATE_INTERPOLATOR = new DecelerateInterpolator();

    private Paint mCirclePaint;
    private float mCircleRadius = 100;
    private float mCirclePointX, mCirclePointY;
    private int mMaxDragHeight = 500;

    private float mProgress;
    private Path mPath = new Path();
    private Paint mPathPaint;
    private int mPathWidth = 4;
    private int mFinalWidth = 200;
    private int mTargetGravityHeight = 100;
    private int mTargetAngle = 120;

    private Paint mPointPaint;
    private int mLeftControlX, mRightControlX, mControlY, mLeftStartX, mRightStartX, mStartY;

    private ValueAnimator mValueAnimator;

    public TouchPullView(final Context context) {
        super(context);
        init();
    }

    public TouchPullView(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TouchPullView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TouchPullView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setAntiAlias(true);
        circlePaint.setDither(true);
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setColor(Color.DKGRAY);
        mCirclePaint = circlePaint;

        final Paint pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pathPaint.setAntiAlias(true);
        pathPaint.setDither(true);
        pathPaint.setStyle(Paint.Style.FILL);
        pathPaint.setStrokeWidth(mPathWidth);
        pathPaint.setColor(Color.GRAY);
        mPathPaint = pathPaint;

        final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setAntiAlias(true);
        pointPaint.setDither(true);
        pointPaint.setStyle(Paint.Style.STROKE);
        pointPaint.setStrokeWidth(20);
        pointPaint.setColor(Color.CYAN);
        mPointPaint = pointPaint;
    }

    public void setProgress(final float progress) {
        mProgress = DECELERATE_INTERPOLATOR.getInterpolation(progress);
        requestLayout();
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // measure width
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int width = MeasureSpec.getSize(widthMeasureSpec);

        int iWidth = (int) (2 * mCircleRadius + getPaddingLeft() + getPaddingRight());
        int measuredWidth;
        if (widthMode == MeasureSpec.EXACTLY) {
            measuredWidth = width;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            measuredWidth = Math.min(iWidth, width);
        } else {
            measuredWidth = iWidth;
        }

        // measure height
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);

        int iHeight = (int) (2 * mCircleRadius + getPaddingTop() + getPaddingBottom()
                        + Math.round(mProgress * mMaxDragHeight));
        int measuredHeight;
        if (heightMode == MeasureSpec.EXACTLY) {
            measuredHeight = height;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            measuredHeight = Math.min(iHeight, height);
        } else {
            measuredHeight = iHeight;
        }

        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updatePathLayout();
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        final int count = canvas.save();
        final float transX = (getWidth() - getDrawableAreaWidth()) / 2.0f;
        canvas.translate(transX, 0);
        canvas.drawPath(mPath, mPathPaint);
        canvas.drawCircle(mCirclePointX, mCirclePointY, mCircleRadius, mCirclePaint);

        if (IS_DEBUG) {
            canvas.drawPoint(mLeftStartX, mStartY, mPointPaint);
            canvas.drawPoint(mRightStartX, mStartY, mPointPaint);
            canvas.drawPoint(mLeftControlX, mControlY, mPointPaint);
            canvas.drawPoint(mRightControlX, mControlY, mPointPaint);
        }

        canvas.restoreToCount(count);
    }

    private void updatePathLayout() {
        final float progress = mProgress;

        // current width and height
        int w = getDrawableAreaWidth();
        int h = (int) (mMaxDragHeight * progress);

        final float cPointx = w / 2.0f;
        final float cRadius = mCircleRadius;
        final float cPointy = h - cRadius;

        // update coordinates
        mCirclePointX = cPointx;
        mCirclePointY = cPointy;

        // reset path
        final Path path = mPath;
        path.reset();

        // find end point's coordinates
        double theta = Math.toRadians(mTargetAngle * progress);
        float dx = cRadius * (float) Math.sin(theta);
        float dy = cRadius * (float) Math.cos(theta);
        final float leftContactX = cPointx - dx;
        final float rightContactX = cPointx + dx;
        final float contactY = cPointy + dy;

        // find control point's coordinates
        final float controlY = mTargetGravityHeight * progress;
        final float deltaY = contactY - controlY;
        final float deltaX = deltaY / (float) Math.tan(theta);
        final float leftControlX = leftContactX - deltaX;
        final float rightControlX = rightContactX + deltaX;

        // define Bezier curve
        path.moveTo(getX(), getY());
        path.quadTo(leftControlX, controlY, leftContactX, contactY);
        path.lineTo(rightContactX, contactY);
        path.quadTo(rightControlX, controlY, getX() + w, getY());

        // debug only
        if (IS_DEBUG) {
            mLeftControlX = (int) leftControlX;
            mRightControlX = (int) rightControlX;
            mControlY = (int) controlY;
            mLeftStartX = (int) getX();
            mRightStartX = (int) getX() + w;
            mStartY = (int) getY();
        }
    }

    private static int getValueByLine(int a, int b, float progress) {
        return a + (int) ((b - a) * progress);
    }

    private int getDrawableAreaWidth() {
        return getValueByLine(getWidth(), mFinalWidth, mProgress);
    }

    public void release() {
        if (mValueAnimator!= null) {
            mValueAnimator.cancel();
            mValueAnimator.setFloatValues(mProgress, 0f);
        } else {
            final ValueAnimator valueAnimator = ValueAnimator.ofFloat(mProgress, 0f);
            valueAnimator.setInterpolator(new DecelerateInterpolator());
            valueAnimator.setDuration(400);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(final ValueAnimator animation) {
                    final Object value = animation.getAnimatedValue();
                    if (value instanceof Float) {
                        setProgress((float) value);
                    }
                }
            });
            mValueAnimator = valueAnimator;
        }
        mValueAnimator.start();
    }
}
