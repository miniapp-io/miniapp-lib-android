package com.hjq.window1;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 *    author : Android Wheel Brother
 *    github : https://github.com/getActivity/EasyWindow
 *    time   : 2021/01/04
 *    desc   : Floating window root layout (handles touch event conflicts)
 */
public final class WindowLayout extends FrameLayout {

    /** Touch event listener */
    private OnTouchListener mOnTouchListener;

    public WindowLayout(Context context) {
        super(context);
    }

    public WindowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WindowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Why write it this way? Some people reported that after setting OnClickListener for child View, the parent View's OnTouchListener does not receive events.
        // After investigation, it was found: the parent View directly dispatches the touch event to the child View's onTouchEvent method in dispatchTouchEvent.
        // As a result, the parent View.OnTouchListener does not receive the event. The solution is to override the touch rules of the View, so that the parent's touch listener takes precedence over the child's click event.
        if (mOnTouchListener != null && mOnTouchListener.onTouch(this, ev)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
        //super.setOnTouchListener(l);
        mOnTouchListener = l;
    }
}