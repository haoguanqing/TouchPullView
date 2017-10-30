package com.ghao.app.ui;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by ghao on 10/22/17. ( ´∀｀)σ
 */

public class TouchPullView extends View {
    private static final boolean IS_DEBUG = false;
    private static final Interpolator DECELERATE_INTERPOLATOR = new DecelerateInterpolator();
    private static final int DEFAULT_COLOR = Color.GRAY;
    private static final float DEFAULT_RADIUS = 100f;
    private static final float DEFAULT_MAX_DRAG_HEIGHT = 500;
    private static final int DEFAULT_FINAL_WIDTH = 200;
    private static final int DEFAULT_FINAL_GRAVITY_HEIGHT = 100;
    private static final int DEFAULT_MAX_ANGLE = 120;

    @NotNull private Paint mCirclePaint;
    private float mCircleRadius;
    private float mCirclePointX, mCirclePointY;
    private float mMaxDragHeight;

    private float mOriginalProgress;
    private float mDeceleratedProgress;
    @NotNull private final Path mPath = new Path();
    @NotNull private Paint mPathPaint;
    private int mFinalWidth;
    private int mFinalControlPointHeight;
    private int mMaxAngle;
    @NotNull private Paint mPointPaint;
    private int mLeftControlX, mRightControlX, mControlY, mLeftStartX, mRightStartX, mStartY;
    @Nullable private ValueAnimator mValueAnimator;
    @ColorInt private int mPrimaryColor;
    @Nullable private Drawable mContentDrawable;
    private int mContentDrawableMargin;

    public TouchPullView(final Context context) {
        super(context);
        init(null);
    }

    public TouchPullView(final Context context, @Nullable final AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public TouchPullView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TouchPullView(final Context context, @Nullable final AttributeSet attrs, final int defStyleAttr, final int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(@Nullable final AttributeSet attrs) {
        final Context context = getContext();
        final TypedArray typedArray = context.obtainStyledAttributes(
                attrs, R.styleable.TouchPullView, 0, 0);
        mPrimaryColor = typedArray.getColor(R.styleable.TouchPullView_primaryColor, DEFAULT_COLOR);
        mCircleRadius = typedArray.getDimension(R.styleable.TouchPullView_radius, DEFAULT_RADIUS);
        mMaxDragHeight = typedArray.getDimension(R.styleable.TouchPullView_maxDragHeight, DEFAULT_MAX_DRAG_HEIGHT);
        mMaxAngle = typedArray.getInt(R.styleable.TouchPullView_maxAngle, DEFAULT_MAX_ANGLE);
        mFinalWidth = typedArray.getInt(R.styleable.TouchPullView_finalWidth, DEFAULT_FINAL_WIDTH);
        mFinalControlPointHeight = typedArray.getInt(R.styleable.TouchPullView_finalGravityHeight, DEFAULT_FINAL_GRAVITY_HEIGHT);
        mContentDrawable = typedArray.getDrawable(R.styleable.TouchPullView_contentDrawable);
        mContentDrawableMargin = typedArray.getDimensionPixelOffset(R.styleable.TouchPullView_contentDrawableMargin, 0);
        typedArray.recycle();

        final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setAntiAlias(true);
        circlePaint.setDither(true);
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setColor(mPrimaryColor);
        mCirclePaint = circlePaint;

        final Paint pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pathPaint.setAntiAlias(true);
        pathPaint.setDither(true);
        pathPaint.setStyle(Paint.Style.FILL);
        pathPaint.setColor(mPrimaryColor);
        mPathPaint = pathPaint;

        // Debugging only: draw control points
        final Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setAntiAlias(true);
        pointPaint.setDither(true);
        pointPaint.setStyle(Paint.Style.STROKE);
        pointPaint.setStrokeWidth(20);
        pointPaint.setColor(Color.CYAN);
        mPointPaint = pointPaint;
    }

    public void setProgress(final float progress) {
        mOriginalProgress = progress;
        mDeceleratedProgress = DECELERATE_INTERPOLATOR.getInterpolation(progress);
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
                + Math.round(mDeceleratedProgress * mMaxDragHeight));
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
        updateContentLayout(mCirclePointX, mCirclePointY, mCircleRadius);
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);

        // translate coordinates
        final int count = canvas.save();
        final float transX = (getWidth() - getDrawableAreaWidth()) / 2.0f;
        canvas.translate(transX, 0);

        // draw contents
        canvas.drawPath(mPath, mPathPaint);
        canvas.drawCircle(mCirclePointX, mCirclePointY, mCircleRadius, mCirclePaint);
        final Drawable drawable = mContentDrawable;
        if (drawable != null) {
            canvas.save();
            canvas.clipRect(drawable.getBounds());
            drawable.draw(canvas);
        }

        if (IS_DEBUG) {
            canvas.drawPoint(mLeftStartX, mStartY, mPointPaint);
            canvas.drawPoint(mRightStartX, mStartY, mPointPaint);
            canvas.drawPoint(mLeftControlX, mControlY, mPointPaint);
            canvas.drawPoint(mRightControlX, mControlY, mPointPaint);
        }

        canvas.restoreToCount(count);
    }

    private void updatePathLayout() {
        final float progress = mDeceleratedProgress;

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
        double theta = Math.toRadians(mMaxAngle * progress);
        float dx = cRadius * (float) Math.sin(theta);
        float dy = cRadius * (float) Math.cos(theta);
        final float leftContactX = cPointx - dx;
        final float rightContactX = cPointx + dx;
        final float contactY = cPointy + dy;

        // find control point's coordinates
        final float controlY = mFinalControlPointHeight * progress;
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

    private void updateContentLayout(float cx, float cy, float radius) {
        final Drawable drawable = mContentDrawable;
        if (drawable != null) {
            final int margin = mContentDrawableMargin;
            final int left = (int) (cx - radius + margin);
            final int right = (int) (cx + radius - margin);
            final int top = (int) (cy - radius + margin);
            final int bottom = (int) (cy + radius - margin);
            drawable.setBounds(left, top, right, bottom);
        }
    }

    private static int getValueByLine(int a, int b, float progress) {
        return a + (int) ((b - a) * progress);
    }

    private int getDrawableAreaWidth() {
        return getValueByLine(getWidth(), mFinalWidth, mDeceleratedProgress);
    }

    public void release() {
        if (mValueAnimator != null) {
            mValueAnimator.cancel();
            mValueAnimator.setFloatValues(mOriginalProgress, 0f);
        } else {
            final ValueAnimator valueAnimator = ValueAnimator.ofFloat(mOriginalProgress, 0f);
            valueAnimator.setInterpolator(new DecelerateInterpolator());
            valueAnimator.setDuration(400L);
            valueAnimator.addUpdateListener(new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(@NotNull final ValueAnimator animation) {
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
