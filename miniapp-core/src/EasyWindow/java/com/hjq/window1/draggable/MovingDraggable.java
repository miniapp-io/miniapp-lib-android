package com.hjq.window1.draggable;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;

/**
 *    author : Android Wheel Brother
 *    github : https://github.com/getActivity/EasyWindow
 *    time   : 2019/01/04
 *    desc   : Implementation class for moving drag handling
 */
public class MovingDraggable extends BaseDraggable {

    /** Coordinates when finger is pressed down */
    private float mViewDownX;
    private float mViewDownY;

    /** Touch move flag */
    private boolean mTouchMoving;

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
     * Whether currently in touch move state
     */
    public boolean isTouchMoving() {
        return mTouchMoving;
    }
}