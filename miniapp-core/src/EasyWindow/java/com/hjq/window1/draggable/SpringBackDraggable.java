package com.hjq.window1.draggable;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import com.hjq.window1.EasyWindow;

/**
 *    author : Android Wheel Brother
 *    github : https://github.com/getActivity/EasyWindow
 *    time   : 2019/01/04
 *    desc   : Implementation class for spring-back after dragging
 */
public class SpringBackDraggable extends BaseDraggable {

    /** Horizontal spring-back */
    public static final int ORIENTATION_HORIZONTAL = LinearLayout.HORIZONTAL;
    /** Vertical spring-back */
    public static final int ORIENTATION_VERTICAL = LinearLayout.VERTICAL;

    /** Coordinates when finger is pressed down */
    private float mViewDownX;
    private float mViewDownY;

    /** Direction of spring-back */
    private final int mSpringBackOrientation;

    /** Touch move flag */
    private boolean mTouchMoving;

    /** Drag spring-back animation listener */
    private SpringBackAnimCallback mSpringBackAnimCallback;

    public SpringBackDraggable() {
        this(ORIENTATION_HORIZONTAL);
    }

    public SpringBackDraggable(int springBackOrientation) {
        mSpringBackOrientation = springBackOrientation;
        switch (mSpringBackOrientation) {
            case LinearLayout.HORIZONTAL:
            case LinearLayout.VERTICAL:
                break;
            default:
                throw new IllegalArgumentException("You cannot pass in directions other than horizontal or vertical");
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Record the position when pressed (relative to the View's coordinates)
                mViewDownX = event.getX();
                mViewDownY = event.getY();
                mTouchMoving = false;
                break;
            case MotionEvent.ACTION_MOVE:
                // Record the position when moving (relative to the screen's coordinates)
                float rawMoveX = event.getRawX() - getWindowInvisibleWidth();
                float rawMoveY = event.getRawY() - getWindowInvisibleHeight();

                float newX = Math.max(rawMoveX - mViewDownX, 0);
                float newY = Math.max(rawMoveY - mViewDownY, 0);

                // Update the position when moving
                updateLocation(newX, newY);

                if (mTouchMoving) {
                    dispatchExecuteDraggingCallback();
                } else if (isFingerMove(mViewDownX, event.getX(), mViewDownY, event.getY())) {
                    // If the user moved their finger, intercept this touch event so that the click event does not take effect
                    mTouchMoving = true;
                    dispatchStartDraggingCallback();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (mTouchMoving) {
                    dispatchSpringBackViewToScreenEdge(event.getRawX(), event.getRawY());
                    dispatchStopDraggingCallback();
                }
                try {
                    return mTouchMoving;
                } finally {
                    // Reset the touch move flag
                    mTouchMoving = false;
                }
            default:
                break;
        }
        return false;
    }

    /**
     * Trigger spring-back of the View to the screen edge
     */
    public void dispatchSpringBackViewToScreenEdge() {
        dispatchSpringBackViewToScreenEdge(getViewOnScreenX(), getViewOnScreenY());
    }

    /**
     * Trigger spring-back of the View to the screen edge
     *
     * @param rawX              Current screen x coordinate of the touch point
     * @param rawY              Current screen y coordinate of the touch point
     */
    public void dispatchSpringBackViewToScreenEdge(float rawX, float rawY) {
        // Record the position when moving (relative to the screen's coordinates)
        float rawMoveX = rawX - getWindowInvisibleWidth();
        float rawMoveY = rawY - getWindowInvisibleHeight();

        // Auto spring-back and snap
        switch (mSpringBackOrientation) {
            case LinearLayout.HORIZONTAL:
                // If moving to the far left, a negative value will be generated, which needs to be handled here because there are no negative coordinates
                float startX = Math.max(rawMoveX - mViewDownX, 0);
                float endX;
                // Get the current screen width
                int screenWidth = getWindowWidth();
                if (rawMoveX < screenWidth / 2f) {
                    // Spring-back to the left edge of the screen
                    endX = 0f;
                } else {
                    // Spring-back to the right edge of the screen (note to subtract the View width, because the coordinate system starts from the top left corner)
                    // If moving to the far right, a negative value will be generated, which needs to be handled here because there are no negative coordinates
                    endX = Math.max((float) screenWidth - getViewWidth(), 0);
                }
                float y = rawMoveY - mViewDownY;
                if (!equalsWithRelativeTolerance(startX, endX)) {
                    // Spring-back from the moving point to the edge
                    startHorizontalAnimation(startX, endX, y);
                }
                break;
            case LinearLayout.VERTICAL:
                float x = rawMoveX - mViewDownX;
                // If moving to the very top, a negative value will be generated, which needs to be handled here because there are no negative coordinates
                float startY = Math.max(rawMoveY - mViewDownY, 0);
                float endY;
                // Get the current screen height
                int screenHeight = getWindowHeight();
                if (rawMoveY < screenHeight / 2f) {
                    // Spring-back to the top edge of the screen
                    endY = 0f;
                } else {
                    // Spring-back to the bottom edge of the screen (note to subtract the View height, because the coordinate system starts from the top left corner)
                    // If moving to the very bottom, a negative value will be generated, which needs to be handled here because there are no negative coordinates
                    endY = Math.max((float) screenHeight - getViewHeight(), 0);
                }
                if (!equalsWithRelativeTolerance(startY, endY)) {
                    // Spring-back from the moving point to the edge
                    startVerticalAnimation(x, startY, endY);
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onScreenRotateInfluenceCoordinateChangeFinish() {
        super.onScreenRotateInfluenceCoordinateChangeFinish();
        dispatchSpringBackViewToScreenEdge();
    }

    /**
     * Determine whether two floating-point numbers are approximately equal (Java floating-point numbers cannot be directly compared with != or ==)
     */
    public boolean equalsWithRelativeTolerance(float number1, float number2) {
        // Define an allowable error range, value is 0.00001
        float epsilon = 1e-5f;
        // Two floating-point numbers are approximately equal
        return Math.abs(number1 - number2) < epsilon;
    }

    public void startHorizontalAnimation(float startX, float endX, final float y) {
        startHorizontalAnimation(startX, endX, y, calculateAnimationDuration(startX, endX));
    }

    /**
     * Execute horizontal spring-back animation
     *
     * @param startX        X-axis start coordinate
     * @param endX          X-axis end coordinate
     * @param y             Y-axis coordinate
     * @param duration      Animation duration
     */
    public void startHorizontalAnimation(float startX, float endX, float y, long duration) {
        startAnimation(startX, endX, duration, animation -> updateLocation((float) animation.getAnimatedValue(), y));
    }

    public void startVerticalAnimation(float x, float startY, float endY) {
        startVerticalAnimation(x, startY, endY, calculateAnimationDuration(startY, endY));
    }

    public void startVerticalAnimation(float x, float startY, float endY, long duration) {
        startAnimation(startY, endY, duration, animation -> updateLocation(x, (float) animation.getAnimatedValue()));
    }

    public void startAnimation(float start, float end, long duration, AnimatorUpdateListener listener) {
        ValueAnimator animator = ValueAnimator.ofFloat(start, end);
        animator.setDuration(duration);
        animator.addUpdateListener(listener);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                dispatchSpringBackAnimationStartCallback(animator);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                dispatchSpringBackAnimationEndCallback(animator);
            }
        });
        animator.start();
    }

    public long calculateAnimationDuration(float startCoordinate, float endCoordinate) {
        return (long) (Math.abs(endCoordinate - startCoordinate) * 1.5f);
    }

    public void setSpringBackAnimCallback(SpringBackAnimCallback callback) {
        mSpringBackAnimCallback = callback;
    }

    protected void dispatchSpringBackAnimationStartCallback(Animator animator) {
        if (mSpringBackAnimCallback != null) {
            mSpringBackAnimCallback.onSpringBackAnimationStart(getEasyWindow(), animator);
        }
    }

    protected void dispatchSpringBackAnimationEndCallback(Animator animator) {
        if (mSpringBackAnimCallback != null) {
            mSpringBackAnimCallback.onSpringBackAnimationEnd(getEasyWindow(), animator);
        }
    }

    public boolean isTouchMoving() {
        return mTouchMoving;
    }

    public interface SpringBackAnimCallback {

        /**
         * Called when the spring-back animation starts
         */
        void onSpringBackAnimationStart(EasyWindow<?> easyWindow, Animator animator);

        /**
         * Called when the spring-back animation ends
         */
        void onSpringBackAnimationEnd(EasyWindow<?> easyWindow, Animator animator);
    }
}