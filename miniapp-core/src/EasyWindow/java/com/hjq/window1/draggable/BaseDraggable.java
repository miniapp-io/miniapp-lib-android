package com.hjq.window1.draggable;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.util.TypedValue;
import android.view.DisplayCutout;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import com.hjq.window1.EasyWindow;

/**
 *    author : Android Wheel Brother
 *    github : https://github.com/getActivity/EasyWindow
 *    time   : 2019/01/04
 *    desc   : Abstract class for drag handling
 */
public abstract class BaseDraggable implements View.OnTouchListener {

    private EasyWindow<?> mEasyWindow;
    private View mDecorView;

    /**
     * Whether to allow moving to the cutout screen area
     */
    private boolean mAllowMoveToScreenNotch = true;

    /**
     * Drag callback listener
     */
    private DraggingCallback mDraggingCallback;

    private final Rect mTempRect = new Rect();

    private int mCurrentWindowWidth;
    private int mCurrentWindowHeight;
    private int mCurrentViewOnScreenX;
    private int mCurrentViewOnScreenY;
    private int mCurrentWindowInvisibleWidth;
    private int mCurrentWindowInvisibleHeight;

    /**
     * Callback after Toast is shown
     */
    @SuppressLint("ClickableViewAccessibility")
    public void start(EasyWindow<?> easyWindow) {
        mEasyWindow = easyWindow;
        mDecorView = easyWindow.getDecorView();
        mDecorView.setOnTouchListener(this);
        mDecorView.post(() -> {
            refreshWindowInfo();
            refreshLocationCoordinate();
        });
    }

    public EasyWindow<?> getEasyWindow() {
        return mEasyWindow;
    }

    public View getDecorView() {
        return mDecorView;
    }

    public void setAllowMoveToScreenNotch(boolean allowMoveToScreenNotch) {
        mAllowMoveToScreenNotch = allowMoveToScreenNotch;
    }

    public boolean isAllowMoveToScreenNotch() {
        return mAllowMoveToScreenNotch;
    }

    /**
     * Get the current Window width
     */
    public int getWindowWidth() {
        return mCurrentWindowWidth;
    }

    /**
     * Get the current Window height
     */
    public int getWindowHeight() {
        return mCurrentWindowHeight;
    }

    /**
     * Get the current View width
     */
    public int getViewWidth() {
        return mEasyWindow.getViewWidth();
    }

    /**
     * Get the current View height
     */
    public int getViewHeight() {
        return mEasyWindow.getViewHeight();
    }

    /**
     * Get the invisible width of the window, usually the height of the notch in landscape mode
     */
    public int getWindowInvisibleWidth() {
        return mCurrentWindowInvisibleWidth;
    }

    /**
     * Get the invisible height of the window, usually the height of the status bar
     */
    public int getWindowInvisibleHeight() {
        return mCurrentWindowInvisibleHeight;
    }

    /**
     * Get the X coordinate of the View on the current screen
     */
    public int getViewOnScreenX() {
        return mCurrentViewOnScreenX;
    }

    /**
     * Get the Y coordinate of the View on the current screen
     */
    public int getViewOnScreenY() {
        return mCurrentViewOnScreenY;
    }

    /**
     * Refresh current Window information
     */
    public void refreshWindowInfo() {
        View decorView = getDecorView();
        if (decorView == null) {
            return;
        }

        // Log.i(getClass().getSimpleName(), "Refresh current Window information");

        // Why write it this way, because it was found that on Harmony phones during screen rotation
        // when the onConfigurationChanged method callback is triggered, these parameters have already changed
        // so we need to record them in advance to avoid problems in subsequent coordinate calculations
        decorView.getWindowVisibleDisplayFrame(mTempRect);
        mCurrentWindowWidth = mTempRect.right - mTempRect.left;
        mCurrentWindowHeight = mTempRect.bottom - mTempRect.top;

        mCurrentWindowInvisibleWidth = mTempRect.left;
        mCurrentWindowInvisibleHeight = mTempRect.top;

        /*
        Log.i(getClass().getSimpleName(),
            "CurrentWindowWidth = " + mCurrentWindowWidth +
            "，CurrentWindowHeight = " + mCurrentWindowHeight +
            "，CurrentWindowInvisibleWidth = " + mCurrentWindowInvisibleWidth +
            "，CurrentWindowInvisibleHeight = " + mCurrentWindowInvisibleHeight);
         */
    }

    /**
     * Refresh current View's coordinate information on screen
     */
    public void refreshLocationCoordinate() {
        View decorView = getDecorView();
        if (decorView == null) {
            return;
        }

        int[] location = new int[2];
        decorView.getLocationOnScreen(location);
        mCurrentViewOnScreenX = location[0];
        mCurrentViewOnScreenY = location[1];
    }

    /**
     * Screen orientation has changed
     */
    public void onScreenOrientationChange() {
        // Log.i(getClass().getSimpleName(), "Screen orientation has changed");

        long refreshDelayMillis = 100;

        if (!isFollowScreenRotationChanges()) {
            getEasyWindow().postDelayed(() -> {
                refreshWindowInfo();
                refreshLocationCoordinate();
            }, refreshDelayMillis);
            return;
        }

        int viewWidth = getDecorView().getWidth();
        int viewHeight = getDecorView().getHeight();

        // Log.i(getClass().getSimpleName(), "Current ViewWidth = " + viewWidth + ", ViewHeight = " + viewHeight);

        int startX = mCurrentViewOnScreenX - mCurrentWindowInvisibleWidth;
        int startY = mCurrentViewOnScreenY - mCurrentWindowInvisibleHeight;

        float percentX;
        // Why use getMinTouchDistance() instead of 0?
        // Because the values measured by getLocationOnScreen are not very accurate, sometimes 0, sometimes 1, sometimes 2
        // but most cases are 0 and 1, so here we use the minimum touch distance as the baseline to accommodate this error
        float minTouchDistance = getMinTouchDistance();

        if (startX <= minTouchDistance) {
            percentX = 0;
        } else if (Math.abs(mCurrentWindowWidth - (startX + viewWidth)) < minTouchDistance) {
            percentX = 1;
        } else {
            float centerX = startX + viewWidth / 2f;
            percentX = centerX / mCurrentWindowWidth;
        }

        float percentY;
        if (startY <= minTouchDistance) {
            percentY = 0;
        } else if (Math.abs(mCurrentWindowHeight - (startY + viewHeight)) < minTouchDistance) {
            percentY = 1;
        } else {
            float centerY = startY + viewHeight / 2f;
            percentY = centerY / mCurrentWindowHeight;
        }

        View decorView = getDecorView();
        if (decorView == null) {
            return;
        }

        // Github issue: https://github.com/getActivity/EasyWindow/issues/49
        // Fix the bug where View.getWindowVisibleDisplayFrame calculation has problems when in portrait mode, first lock screen, then rotate to landscape, then unlock
        // This is because during screen rotation, the view is in a changing state, and getting the window visible area through View at this time is problematic, it will get the old visible area
        // The solution is to listen to the View layout change listener, and get the View window visible area when the callback is received
        decorView.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View view, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                view.removeOnLayoutChangeListener(this);
                view.postDelayed(() -> {
                    // First refresh current window information
                    refreshWindowInfo();
                    int x = Math.max((int) (mCurrentWindowWidth * percentX - viewWidth / 2f), 0);
                    int y = Math.max((int) (mCurrentWindowHeight * percentY - viewWidth / 2f), 0);
                    updateLocation(x, y);
                    // Note: this needs to be executed with delay, otherwise there will be problems
                    view.post(() -> onScreenRotateInfluenceCoordinateChangeFinish());
                }, refreshDelayMillis);
            }
        });
    }

    /**
     * Method called when screen rotation causes floating window coordinate changes to complete
     */
    protected void onScreenRotateInfluenceCoordinateChangeFinish() {
        refreshWindowInfo();
        refreshLocationCoordinate();
    }

    /**
     * Whether the floating window changes with screen orientation changes
     */
    public boolean isFollowScreenRotationChanges() {
        return true;
    }

    public void updateLocation(float x, float y) {
        updateLocation(x, y, isAllowMoveToScreenNotch());
    }

    public void updateLocation(float x, float y, boolean allowMoveToScreenNotch) {
        updateLocation((int) x, (int) y, allowMoveToScreenNotch);
    }

    /**
     * Update floating window position
     *
     * @param x                                 x coordinate (relative to screen top-left position)
     * @param y                                 y coordinate (relative to screen top-left position)
     * @param allowMoveToScreenNotch            whether to allow moving to the cutout screen area
     */
    public void updateLocation(int x, int y, boolean allowMoveToScreenNotch) {
        if (allowMoveToScreenNotch) {
            updateWindowCoordinate(x, y);
            return;
        }

        Rect safeInsetRect = getSafeInsetRect();
        if (safeInsetRect == null) {
            updateWindowCoordinate(x, y);
            return;
        }

        if (safeInsetRect.left > 0 && safeInsetRect.right > 0 &&
            safeInsetRect.top > 0 && safeInsetRect.bottom > 0) {
            updateWindowCoordinate(x, y);
            return;
        }

        int viewWidth = mEasyWindow.getViewWidth();
        int viewHeight = mEasyWindow.getViewHeight();

        int windowWidth = getWindowWidth();
        int windowHeight = getWindowHeight();

        // Log.i(getClass().getSimpleName(), "Start x coordinate: " + x);
        // Log.i(getClass().getSimpleName(), "Start y coordinate: " + y);

        if (x < safeInsetRect.left - getWindowInvisibleWidth()) {
            x = safeInsetRect.left - getWindowInvisibleWidth();
            // Log.i(getClass().getSimpleName(), "x coordinate has touched the left safe area of the screen");
        } else if (x > windowWidth - safeInsetRect.right - viewWidth) {
            x = windowWidth - safeInsetRect.right - viewWidth;
            // Log.i(getClass().getSimpleName(), "x coordinate has touched the right safe area of the screen");
        }

        // Log.i(getClass().getSimpleName(), "Final x coordinate: " + x);

        if (y < safeInsetRect.top - getWindowInvisibleHeight()) {
            y = safeInsetRect.top - getWindowInvisibleHeight();
            // Log.i(getClass().getSimpleName(), "y coordinate has touched the top safe area of the screen");
        } else if (y > windowHeight - safeInsetRect.bottom - viewHeight) {
            y = windowHeight - safeInsetRect.bottom - viewHeight;
            // Log.i(getClass().getSimpleName(), "y coordinate has touched the bottom safe area of the screen");
        }

        // Log.i(getClass().getSimpleName(), "Final y coordinate: " + y);

        updateWindowCoordinate(x, y);
    }

    public void updateWindowCoordinate(int x, int y) {
        WindowManager.LayoutParams params = mEasyWindow.getWindowParams();
        if (params == null) {
            return;
        }

        // Screen default center (must first set center position to top-left corner)
        int screenGravity = Gravity.TOP | Gravity.START;

        // Determine if the current move position is consistent with the current window position
        if (params.gravity == screenGravity && params.x == x && params.y == y) {
            return;
        }

        params.x = x;
        params.y = y;
        params.gravity = screenGravity;

        mEasyWindow.update();
        refreshLocationCoordinate();
    }

    public Rect getSafeInsetRect() {
        Context context = mEasyWindow.getContext();
        Window window;
        if (!(context instanceof Activity)) {
            return null;
        }

        window = ((Activity) context).getWindow();
        if (window == null) {
            return null;
        }

        return getSafeInsetRect(window);
    }

    /**
     * Get screen safe area position (returned object may be null)
     */
    public static Rect getSafeInsetRect(Window window) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return null;
        }

        View activityDecorView = null;
        if (window != null) {
            activityDecorView = window.getDecorView();
        }
        WindowInsets rootWindowInsets = null;
        if (activityDecorView != null) {
            rootWindowInsets = activityDecorView.getRootWindowInsets();
        }
        DisplayCutout displayCutout = null;
        if (rootWindowInsets != null) {
            displayCutout = rootWindowInsets.getDisplayCutout();
        }

        if (displayCutout != null) {
            // Safe area distance from screen left
            int safeInsetLeft = displayCutout.getSafeInsetLeft();
            // Safe area distance from screen top
            int safeInsetTop = displayCutout.getSafeInsetTop();
            // Safe area distance from screen right
            int safeInsetRight = displayCutout.getSafeInsetRight();
            // Safe area distance from screen bottom
            int safeInsetBottom = displayCutout.getSafeInsetBottom();

            // Log.i(getClass().getSimpleName(), "Safe area distance from screen left: " + safeInsetLeft);
            // Log.i(getClass().getSimpleName(), "Safe area distance from screen right: " + safeInsetRight);
            // Log.i(getClass().getSimpleName(), "Safe area distance from screen top: " + safeInsetTop);
            // Log.i(getClass().getSimpleName(), "Safe area distance from screen bottom: " + safeInsetBottom);

            return new Rect(safeInsetLeft, safeInsetTop, safeInsetRight, safeInsetBottom);
        }

        return null;
    }

    /**
     * Determine if the user's finger has moved, judgment criteria as follows:
     * Judge based on the coordinates when the finger is pressed down and lifted up, cannot judge based on whether there is a move event
     * Because on some devices, even if the user doesn't move their finger, just a simple click will also generate a move event
     *
     * @param downX         x coordinate when finger is pressed down
     * @param upX           x coordinate when finger is lifted up
     * @param downY         y coordinate when finger is pressed down
     * @param upY           y coordinate when finger is lifted up
     */
    protected boolean isFingerMove(float downX, float upX, float downY, float upY) {
        float minTouchSlop = getMinTouchDistance();
        return Math.abs(downX - upX) >= minTouchSlop || Math.abs(downY - upY) >= minTouchSlop;
    }

    /**
     * Get minimum touch distance
     */
    protected float getMinTouchDistance() {
        // Question 1: Why use 1dp as the minimum touch distance?
        //         This is because when the user clicks, the coordinates of finger down and up are not equal, there will be some error
        //         On some phones, the error will be smaller, and on some phones, the error will be larger
        //         After testing and verifying with different phones, this error value can be locked within 1dp
        //         Of course, my conclusion may not be correct, if you find new problems you can also give me feedback, I will continue to optimize this issue
        // Question 2: Why not use ViewConfiguration.get(context).getScaledTouchSlop()?
        //         This is because the value obtained by this API is too large, there is a certain probability of misjudgment, on the same phone
        //         getScaledTouchSlop gets 24, while system 1dp gets 3,
        //         The difference is too large, because getScaledTouchSlop API defaults to 8dp * 3 = 24px
        // Question 3: Why use Resources.getSystem to get, instead of context.getResources?
        //         This is because if you use the AutoSize framework, 1dp in the context is no longer 3px
        //         Using Resources.getSystem ensures that the Resources object dp calculation rules are not tampered with by third-party frameworks
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1,
                Resources.getSystem().getDisplayMetrics());
    }

    /**
     * Set drag callback
     */
    public void setDraggingCallback(DraggingCallback callback) {
        mDraggingCallback = callback;
    }

    /**
     * Dispatch start drag event
     */
    protected void dispatchStartDraggingCallback() {
        // Log.i(getClass().getSimpleName(), "Start dragging");
        if (mDraggingCallback == null) {
            return;
        }
        mDraggingCallback.onStartDragging(mEasyWindow);
    }

    /**
     * Dispatch drag in progress event
     */
    protected void dispatchExecuteDraggingCallback() {
        // Log.i(getClass().getSimpleName(), "Dragging in progress");
        if (mDraggingCallback == null) {
            return;
        }
        mDraggingCallback.onExecuteDragging(mEasyWindow);
    }

    /**
     * Dispatch stop drag event
     */
    protected void dispatchStopDraggingCallback() {
        // Log.i(getClass().getSimpleName(), "Stop dragging");
        if (mDraggingCallback == null) {
            return;
        }
        mDraggingCallback.onStopDragging(mEasyWindow);
    }

    public interface DraggingCallback {

        /**
         * Start dragging
         */
        default void onStartDragging(EasyWindow<?> easyWindow) {}

        /**
         * Execute dragging in progress
         */
        default void onExecuteDragging(EasyWindow<?> easyWindow) {}

        /**
         * Stop dragging
         */
        default void onStopDragging(EasyWindow<?> easyWindow) {}
    }
}