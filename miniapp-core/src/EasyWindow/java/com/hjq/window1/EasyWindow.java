package com.hjq.window1;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.hjq.window1.draggable.BaseDraggable;
import com.hjq.window1.draggable.MovingDraggable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *    author : Android Wheel Brother
 *    github : https://github.com/getActivity/EasyWindow
 *    time   : 2019/01/04
 *    desc   : Floating window framework
 *    doc    : https://developer.android.google.cn/reference/android/view/WindowManager.html
 *             https://developer.android.google.cn/reference/kotlin/android/view/WindowManager.LayoutParams?hl=en
 */
@SuppressWarnings({"unchecked", "unused", "UnusedReturnValue"})
public class EasyWindow<X extends EasyWindow<?>> implements Runnable,
        ScreenOrientationMonitor.OnScreenOrientationCallback {

    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    private static final List<EasyWindow<?>> sWindowInstanceSet = new ArrayList<>();

    /**
     * Create an EasyWindow instance based on Activity
     */
    @SuppressWarnings("rawtypes")
    public static EasyWindow with(Activity activity) {
        return new EasyWindow(activity);
    }

    /**
     * Create an EasyWindow instance based on global context, requires floating window permission
     */
    @SuppressWarnings("rawtypes")
    public static EasyWindow with(Application application) {
        return new EasyWindow(application);
    }

    /**
     * Cancel all currently displayed floating windows
     */
    public static synchronized void cancelAll() {
        for (EasyWindow<?> easyWindow : sWindowInstanceSet) {
            if (easyWindow == null) {
                continue;
            }
            easyWindow.cancel();
        }
    }

    /**
     * Cancel floating windows with specific class name
     */
    public static synchronized void cancelByClass(Class<? extends EasyWindow<?>> clazz) {
        if (clazz == null) {
            return;
        }
        for (EasyWindow<?> easyWindow : sWindowInstanceSet) {
            if (easyWindow == null) {
                continue;
            }
            if (!clazz.equals(easyWindow.getClass())) {
                continue;
            }
            easyWindow.cancel();
        }
    }

    /**
     * Cancel floating windows with specific tag
     */
    public static synchronized void cancelByTag(String tag) {
        if (tag == null) {
            return;
        }
        for (EasyWindow<?> easyWindow : sWindowInstanceSet) {
            if (easyWindow == null) {
                continue;
            }
            if (!tag.equals(easyWindow.getTag())) {
                continue;
            }
            easyWindow.cancel();
        }
    }

    /**
     * Recycle all currently displayed floating windows
     */
    public static synchronized void recycleAll() {
        Iterator<EasyWindow<?>> iterator = sWindowInstanceSet.iterator();
        while (iterator.hasNext()) {
            EasyWindow<?> easyWindow = iterator.next();
            if (easyWindow == null) {
                continue;
            }
            // Here's why we use iterator removal, if we don't do it this way
            // the easyWindow.recycle method will remove it again
            // and since we're in a while loop, there might be an index out of bounds situation
            iterator.remove();
            easyWindow.recycle();
        }
    }

    /**
     * Recycle floating windows with specific class name
     */
    public static synchronized void recycleByClass(Class<? extends EasyWindow<?>> clazz) {
        if (clazz == null) {
            return;
        }
        Iterator<EasyWindow<?>> iterator = sWindowInstanceSet.iterator();
        while (iterator.hasNext()) {
            EasyWindow<?> easyWindow = iterator.next();
            if (easyWindow == null) {
                continue;
            }
            if (!clazz.equals(easyWindow.getClass())) {
                continue;
            }
            // Here's why we use iterator removal, if we don't do it this way
            // the easyWindow.recycle method will remove it again
            // and since we're in a while loop, there might be an index out of bounds situation
            iterator.remove();
            easyWindow.recycle();
        }
    }

    /**
     * Recycle floating windows with specific tag
     */
    public static synchronized void recycleByTag(String tag) {
        if (tag == null) {
            return;
        }

        Iterator<EasyWindow<?>> iterator = sWindowInstanceSet.iterator();
        while (iterator.hasNext()) {
            EasyWindow<?> easyWindow = iterator.next();
            if (easyWindow == null) {
                continue;
            }
            if (!tag.equals(easyWindow.getTag())) {
                continue;
            }
            // Here's why we use iterator removal, if we don't do it this way
            // the easyWindow.recycle method will remove it again
            // and since we're in a while loop, there might be an index out of bounds situation
            iterator.remove();
            easyWindow.recycle();
        }
    }

    /**
     * Determine if there is currently a floating window being displayed
     */
    public static synchronized boolean existShowing() {
        Iterator<EasyWindow<?>> iterator = sWindowInstanceSet.iterator();
        while (iterator.hasNext()) {
            EasyWindow<?> easyWindow = iterator.next();
            if (easyWindow == null) {
                continue;
            }

            if (easyWindow.isShowing()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if there is currently a floating window with specific class name being displayed
     */
    public static synchronized boolean existShowingByClass(Class<? extends EasyWindow<?>> clazz) {
        if (clazz == null) {
            return false;
        }
        Iterator<EasyWindow<?>> iterator = sWindowInstanceSet.iterator();
        while (iterator.hasNext()) {
            EasyWindow<?> easyWindow = iterator.next();
            if (easyWindow == null) {
                continue;
            }
            if (!clazz.equals(easyWindow.getClass())) {
                continue;
            }
            if (easyWindow.isShowing()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if there is currently a floating window with specific tag being displayed
     */
    public static synchronized boolean existShowingByTag(String tag) {
        if (tag == null) {
            return false;
        }

        Iterator<EasyWindow<?>> iterator = sWindowInstanceSet.iterator();
        while (iterator.hasNext()) {
            EasyWindow<?> easyWindow = iterator.next();
            if (easyWindow == null) {
                continue;
            }
            if (!tag.equals(easyWindow.getTag())) {
                continue;
            }
            if (easyWindow.isShowing()) {
                return true;
            }
        }
        return false;
    }

    /** Context */
    private Context mContext;
    /** Root layout */
    private ViewGroup mDecorView;
    /** Floating window */
    private WindowManager mWindowManager;
    /** Floating window parameters */
    private WindowManager.LayoutParams mWindowParams;

    /** Whether it is currently displayed */
    private boolean mShowing;
    /** Floating window display duration */
    private int mDuration;
    /** Floating window tag */
    private String mTag;
    /** Toast lifecycle management */
    private ActivityWindowLifecycle mActivityWindowLifecycle;
    /** Custom drag processing */
    private BaseDraggable mDraggable;
    /** Toast display and cancel listener */
    private OnWindowLifecycle mOnWindowLifecycle;

    /** Screen rotation listener */
    private ScreenOrientationMonitor mScreenOrientationMonitor;

    /** Update task */
    private final Runnable mUpdateRunnable = this::update;

    /**
     * Create a local floating window
     */
    public EasyWindow(Activity activity) {
        this((Context) activity);

        Window window = activity.getWindow();
        View decorView = window.getDecorView();
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        if ((params.flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0 ||
                (decorView.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0) {
            // If the current Activity is in fullscreen mode, this flag needs to be added, otherwise WindowManager may not be able to move to the status bar position on some devices
            // If you don't want the status bar to push down the WindowManager when it appears, you can add FLAG_LAYOUT_IN_SCREEN, but this will cause the soft keyboard to not adjust the window position
            addWindowFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // If it is Android 9.0, adaptation for notch screens is required, otherwise WindowManager may not be able to move to the notch area
            setLayoutInDisplayCutoutMode(params.layoutInDisplayCutoutMode);
        }

        if (params.systemUiVisibility != 0) {
            setSystemUiVisibility(params.systemUiVisibility);
        }

        if (decorView.getSystemUiVisibility() != 0) {
            mDecorView.setSystemUiVisibility(decorView.getSystemUiVisibility());
        }

        // Follow the Activity lifecycle
        mActivityWindowLifecycle = new ActivityWindowLifecycle(this, activity);
        // Register Activity lifecycle listener
        mActivityWindowLifecycle.register();
    }

    /**
     * Create a global floating window
     */
    public EasyWindow(Application application) {
        this((Context) application);

        // Set as a global floating window, note that you need to apply for floating window permission first, recommended: https://github.com/getActivity/XXPermissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setWindowType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        } else {
            setWindowType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
    }

    private EasyWindow(Context context) {
        mContext = context;
        mDecorView = new WindowLayout(context);
        mWindowManager = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
        // Configure some default parameters
        mWindowParams = new WindowManager.LayoutParams();
        mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mWindowParams.format = PixelFormat.TRANSLUCENT;
        mWindowParams.windowAnimations = android.R.style.Animation_Toast;
        mWindowParams.packageName = context.getPackageName();
        // Set touch outer layout (except for WindowManager's layout, by default the outer layer is not touchable when WindowManager is displayed)
        // Note that if FLAG_NOT_TOUCH_MODAL is set, FLAG_NOT_FOCUSABLE must also be set, otherwise the back button will not work
        mWindowParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        // Add the current instance to the static collection
        sWindowInstanceSet.add(this);
    }

    /**
     * Set floating window tag
     */
    public X setTag(String tag) {
        mTag = tag;
        return (X) this;
    }

    /**
     * Set floating window width
     */
    public X setWidth(int width) {
        mWindowParams.width = width;
        if (mDecorView.getChildCount() > 0) {
            View contentView = mDecorView.getChildAt(0);
            ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
            if (layoutParams != null && layoutParams.width != width) {
                layoutParams.width = width;
                contentView.setLayoutParams(layoutParams);
            }
        }
        postUpdate();
        return (X) this;
    }

    /**
     * Set floating window height
     */
    public X setHeight(int height) {
        mWindowParams.height = height;
        if (mDecorView.getChildCount() > 0) {
            View contentView = mDecorView.getChildAt(0);
            ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
            if (layoutParams != null && layoutParams.height != height) {
                layoutParams.height = height;
                contentView.setLayoutParams(layoutParams);
            }
        }
        postUpdate();
        return (X) this;
    }

    /**
     * Set floating window display center
     */
    public X setGravity(int gravity) {
        mWindowParams.gravity = gravity;
        postUpdate();
        post(() -> {
            if (mDraggable != null) {
                mDraggable.refreshLocationCoordinate();
            }
        });
        return (X) this;
    }

    /**
     * Set horizontal offset
     */
    public X setXOffset(int px) {
        mWindowParams.x = px;
        postUpdate();
        post(() -> {
            if (mDraggable != null) {
                mDraggable.refreshLocationCoordinate();
            }
        });
        return (X) this;
    }

    /**
     * Set vertical offset
     */
    public X setYOffset(int px) {
        mWindowParams.y = px;
        postUpdate();
        post(() -> {
            if (mDraggable != null) {
                mDraggable.refreshLocationCoordinate();
            }
        });
        return (X) this;
    }

    /**
     * Set whether the outer layer of floating window is touchable
     */
    public X setOutsideTouchable(boolean touchable) {
        int flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if (touchable) {
            addWindowFlags(flags);
        } else {
            removeWindowFlags(flags);
        }
        postUpdate();
        return (X) this;
    }

    /**
     * Set floating window background shadow intensity
     *
     * @param amount         Shadow intensity value, fill in a value between 0 and 1
     */
    public X setBackgroundDimAmount(float amount) {
        if (amount < 0 || amount > 1) {
            throw new IllegalArgumentException("amount must be a value between 0 and 1");
        }
        mWindowParams.dimAmount = amount;
        int flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        if (amount != 0) {
            addWindowFlags(flags);
        } else {
            removeWindowFlags(flags);
        }
        postUpdate();
        return (X) this;
    }

    /**
     * Add window tag
     */
    public X addWindowFlags(int flags) {
        mWindowParams.flags |= flags;
        postUpdate();
        return (X) this;
    }

    /**
     * Remove window tag
     */
    public X removeWindowFlags(int flags) {
        mWindowParams.flags &= ~flags;
        postUpdate();
        return (X) this;
    }

    /**
     * Set window tag
     */
    public X setWindowFlags(int flags) {
        mWindowParams.flags = flags;
        postUpdate();
        return (X) this;
    }

    /**
     * Whether there is a window tag
     */
    public boolean hasWindowFlags(int flags) {
        return (mWindowParams.flags & flags) != 0;
    }

    /**
     * Set floating window display type
     */
    public X setWindowType(int type) {
        mWindowParams.type = type;
        postUpdate();
        return (X) this;
    }

    /**
     * Set animation style
     */
    public X setAnimStyle(int id) {
        mWindowParams.windowAnimations = id;
        postUpdate();
        return (X) this;
    }

    /**
     * Set soft keyboard mode
     *
     * {@link WindowManager.LayoutParams#SOFT_INPUT_STATE_UNSPECIFIED}: No specified state, the system will choose a suitable state or depend on the theme setting
     * {@link WindowManager.LayoutParams#SOFT_INPUT_STATE_UNCHANGED}: Will not change the soft keyboard state
     * {@link WindowManager.LayoutParams#SOFT_INPUT_STATE_HIDDEN}: The soft keyboard is default hidden when the user enters the window
     * {@link WindowManager.LayoutParams#SOFT_INPUT_STATE_ALWAYS_HIDDEN}: The soft keyboard is always hidden when the window gets focus
     * {@link WindowManager.LayoutParams#SOFT_INPUT_ADJUST_RESIZE}: The window will adjust size when the soft keyboard pops up
     * {@link WindowManager.LayoutParams#SOFT_INPUT_ADJUST_PAN}: The window does not need to adjust size when the soft keyboard pops up, ensure that the input focus is visible
     */
    public X setSoftInputMode(int mode) {
        mWindowParams.softInputMode = mode;
        // If you set it to be untouchable, erase this mark, otherwise it will cause the input method to not pop up
        removeWindowFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        postUpdate();
        return (X) this;
    }

    /**
     * Set floating window Token
     */
    public X setWindowToken(IBinder token) {
        mWindowParams.token = token;
        postUpdate();
        return (X) this;
    }

    /**
     * Set floating window transparency
     */
    public X setWindowAlpha(float alpha) {
        mWindowParams.alpha = alpha;
        postUpdate();
        return (X) this;
    }

    /**
     * Set vertical spacing
     */
    public X setVerticalMargin(float verticalMargin) {
        mWindowParams.verticalMargin = verticalMargin;
        postUpdate();
        return (X) this;
    }

    /**
     * Set horizontal spacing
     */
    public X setHorizontalMargin(float horizontalMargin) {
        mWindowParams.horizontalMargin = horizontalMargin;
        postUpdate();
        return (X) this;
    }

    /**
     * Set bitmap format
     */
    public X setBitmapFormat(int format) {
        mWindowParams.format = format;
        postUpdate();
        return (X) this;
    }

    /**
     * Set system bar visibility
     */
    public X setSystemUiVisibility(int systemUiVisibility) {
        mWindowParams.systemUiVisibility = systemUiVisibility;
        postUpdate();
        return (X) this;
    }

    /**
     * Set vertical weight
     */
    public X setVerticalWeight(float verticalWeight) {
        mWindowParams.verticalWeight = verticalWeight;
        postUpdate();
        return (X) this;
    }

    /**
     * Set display mode under cutout screen
     */
    public X setLayoutInDisplayCutoutMode(int mode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mWindowParams.layoutInDisplayCutoutMode = mode;
            postUpdate();
        }
        return (X) this;
    }

    /**
     * Set floating window display screen
     */
    public X setPreferredDisplayModeId(int id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mWindowParams.preferredDisplayModeId = id;
            postUpdate();
        }
        return (X) this;
    }

    /**
     * Set floating window title
     */
    public X setWindowTitle(CharSequence title) {
        mWindowParams.setTitle(title);
        postUpdate();
        return (X) this;
    }

    /**
     * Set screen brightness
     */
    public X setScreenBrightness(float screenBrightness) {
        mWindowParams.screenBrightness = screenBrightness;
        postUpdate();
        return (X) this;
    }

    /**
     * Set button brightness
     */
    public X setButtonBrightness(float buttonBrightness) {
        mWindowParams.buttonBrightness = buttonBrightness;
        postUpdate();
        return (X) this;
    }

    /**
     * Set the refresh rate of the floating window
     */
    public X setPreferredRefreshRate(float preferredRefreshRate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mWindowParams.preferredRefreshRate = preferredRefreshRate;
            postUpdate();
        }
        return (X) this;
    }

    /**
     * Set the color mode of the floating window
     */
    public X setColorMode(int colorMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mWindowParams.setColorMode(colorMode);
            postUpdate();
        }
        return (X) this;
    }

    /**
     * Set the Gaussian blur radius of the floating window (only available on Android 12)
     */
    public X setBlurBehindRadius(int blurBehindRadius) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            mWindowParams.setBlurBehindRadius(blurBehindRadius);
            addWindowFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            postUpdate();
        }
        return (X) this;
    }

    /**
     * Set the screen orientation of the floating window
     *
     * Adaptive: {@link ActivityInfo#SCREEN_ORIENTATION_UNSPECIFIED}
     * Landscape: {@link ActivityInfo#SCREEN_ORIENTATION_LANDSCAPE}
     * Portrait: {@link ActivityInfo#SCREEN_ORIENTATION_PORTRAIT}
     */
    public X setScreenOrientation(int orientation) {
        mWindowParams.screenOrientation = orientation;
        postUpdate();
        return (X) this;
    }

    /**
     * Reset WindowManager parameter set
     */
    public X setWindowParams(WindowManager.LayoutParams params) {
        mWindowParams = params;
        postUpdate();
        return (X) this;
    }

    /**
     * Set free drag
     */
    public X setDraggable() {
        return setDraggable(new MovingDraggable());
    }

    /**
     * Set drag rules
     */
    public X setDraggable(BaseDraggable draggable) {
        mDraggable = draggable;
        if (draggable != null) {
            // If currently set as not touchable, remove this flag
            removeWindowFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            // If currently set as movable window to outside the screen, remove this flag
            removeWindowFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

            if (isShowing()) {
                update();
                draggable.start(this);
            }
        }

        if (mScreenOrientationMonitor == null) {
            mScreenOrientationMonitor = new ScreenOrientationMonitor(mContext.getResources().getConfiguration());
        }
        mScreenOrientationMonitor.registerCallback(mContext, this);

        return (X) this;
    }

    /**
     * Limit display duration
     */
    public X setDuration(int duration) {
        mDuration = duration;
        if (isShowing() && mDuration != 0) {
            removeCallbacks(this);
            postDelayed(this, mDuration);
        }
        return (X) this;
    }

    /**
     * Set lifecycle listener
     */
    public X setOnWindowLifecycle(OnWindowLifecycle listener) {
        mOnWindowLifecycle = listener;
        return (X) this;
    }

    /**
     * Set root layout (generally recommend using {@link #setContentView} to fill the layout)
     */
    public X setDecorView(ViewGroup viewGroup) {
        mDecorView = viewGroup;
        return (X) this;
    }

    /**
     * Set content layout
     */
    public X setContentView(int id) {
        return setContentView(LayoutInflater.from(mContext).inflate(id, mDecorView, false));
    }

    public X setContentView(View view) {
        if (mDecorView.getChildCount() > 0) {
            mDecorView.removeAllViews();
        }
        mDecorView.addView(view);

        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams marginLayoutParams = ((ViewGroup.MarginLayoutParams) layoutParams);
            // Clear Margin, because WindowManager does not have this property and it will conflict with the root layout
            marginLayoutParams.topMargin = 0;
            marginLayoutParams.bottomMargin = 0;
            marginLayoutParams.leftMargin = 0;
            marginLayoutParams.rightMargin = 0;
        }

        // If no gravity is currently set, automatically get the layout gravity
        if (mWindowParams.gravity == Gravity.NO_GRAVITY) {
            if (layoutParams instanceof FrameLayout.LayoutParams) {
                int gravity = ((FrameLayout.LayoutParams) layoutParams).gravity;
                if (gravity != FrameLayout.LayoutParams.UNSPECIFIED_GRAVITY) {
                    mWindowParams.gravity = gravity;
                }
            } else if (layoutParams instanceof LinearLayout.LayoutParams) {
                int gravity = ((LinearLayout.LayoutParams) layoutParams).gravity;
                if (gravity != FrameLayout.LayoutParams.UNSPECIFIED_GRAVITY) {
                    mWindowParams.gravity = gravity;
                }
            }

            if (mWindowParams.gravity == Gravity.NO_GRAVITY) {
                // Default gravity is center
                mWindowParams.gravity = Gravity.CENTER;
            }
        }

        if (layoutParams != null) {
            if (mWindowParams.width == WindowManager.LayoutParams.WRAP_CONTENT &&
                    mWindowParams.height == WindowManager.LayoutParams.WRAP_CONTENT) {
                // If the current Dialog width and height are set to wrap_content, use the width and height set in the layout
                mWindowParams.width = layoutParams.width;
                mWindowParams.height = layoutParams.height;
            } else {
                // If the width and height are dynamically set by code, use the dynamically set values
                layoutParams.width = mWindowParams.width;
                layoutParams.height = mWindowParams.height;
            }
        }

        postUpdate();
        return (X) this;
    }

    public void showAsDropDown(View anchorView) {
        showAsDropDown(anchorView, Gravity.BOTTOM);
    }

    public void showAsDropDown(View anchorView, int showGravity) {
        showAsDropDown(anchorView, showGravity, 0 , 0);
    }

    /**
     * Show the floating window below a specific View (similar to the method with the same name in PopupWindow)
     */
    public void showAsDropDown(View anchorView, int showGravity, int xOff, int yOff) {
        if (mDecorView.getChildCount() == 0 || mWindowParams == null) {
            throw new IllegalArgumentException("WindowParams and view cannot be empty");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Adapt to layout direction
            showGravity = Gravity.getAbsoluteGravity(showGravity,
                    anchorView.getResources().getConfiguration().getLayoutDirection());
        }

        int[] anchorViewLocation = new int[2];
        anchorView.getLocationOnScreen(anchorViewLocation);

        Rect windowVisibleRect = new Rect();
        anchorView.getWindowVisibleDisplayFrame(windowVisibleRect);

        mWindowParams.gravity = Gravity.TOP | Gravity.START;
        mWindowParams.x = anchorViewLocation[0] - windowVisibleRect.left + xOff;
        mWindowParams.y = anchorViewLocation[1] - windowVisibleRect.top + yOff;

        if ((showGravity & Gravity.LEFT) == Gravity.LEFT) {
            int rootViewWidth = mDecorView.getWidth();
            if (rootViewWidth == 0) {
                rootViewWidth = mDecorView.getMeasuredWidth();
            }
            if (rootViewWidth == 0) {
                mDecorView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                rootViewWidth = mDecorView.getMeasuredWidth();
            }
            mWindowParams.x -= rootViewWidth;
        } else if ((showGravity & Gravity.RIGHT) == Gravity.RIGHT) {
            mWindowParams.x += anchorView.getWidth();
        }

        if ((showGravity & Gravity.TOP) == Gravity.TOP) {
            int rootViewHeight = mDecorView.getHeight();
            if (rootViewHeight == 0) {
                rootViewHeight = mDecorView.getMeasuredHeight();
            }
            if (rootViewHeight == 0) {
                mDecorView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                rootViewHeight = mDecorView.getMeasuredHeight();
            }
            mWindowParams.y -= rootViewHeight;
        } else if ((showGravity & Gravity.BOTTOM) == Gravity.BOTTOM) {
            mWindowParams.y += anchorView.getHeight();
        }

        show();
    }

    /**
     * Show the floating window
     */
    public void show() {
        if (mDecorView.getChildCount() == 0 || mWindowParams == null) {
            throw new IllegalArgumentException("WindowParams and view cannot be empty");
        }

        // If already showing, update
        if (mShowing) {
            update();
            return;
        }

        if (mContext instanceof Activity) {
            Activity activity = ((Activity) mContext);
            if (activity.isFinishing() ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 &&
                            activity.isDestroyed())) {
                return;
            }
        }

        try {
            // If the View has already been added, remove it first
            if (mDecorView.getParent() != null) {
                mWindowManager.removeViewImmediate(mDecorView);
            }
            mWindowManager.addView(mDecorView, mWindowParams);
            // Currently displayed
            mShowing = true;
            // If a display duration is set
            if (mDuration != 0) {
                removeCallbacks(this);
                postDelayed(this, mDuration);
            }
            // If drag rules are set
            if (mDraggable != null) {
                mDraggable.start(this);
            }

            // Callback listener
            if (mOnWindowLifecycle != null) {
                mOnWindowLifecycle.onWindowShow(this);
            }

        } catch (NullPointerException | IllegalStateException |
                IllegalArgumentException | WindowManager.BadTokenException e) {
            // If this View object is added to WindowManager repeatedly, an exception will be thrown
            e.printStackTrace();
        }
    }

    /**
     * Destroy the floating window
     */
    public void cancel() {
        if (!mShowing) {
            return;
        }

        try {
            // If the current WindowManager does not attach this View, an exception will be thrown
            mWindowManager.removeViewImmediate(mDecorView);

            // Remove destroy task
            removeCallbacks(this);

            // Callback listener
            if (mOnWindowLifecycle != null) {
                mOnWindowLifecycle.onWindowCancel(this);
            }

        } catch (NullPointerException | IllegalArgumentException | IllegalStateException e) {
            e.printStackTrace();
        } finally {
            // Not currently displayed
            mShowing = false;
        }
    }

    /**
     * Delay update of the floating window
     */
    public void postUpdate() {
        if (!isShowing()) {
            return;
        }
        removeCallbacks(mUpdateRunnable);
        post(mUpdateRunnable);
    }

    /**
     * Update the floating window
     */
    public void update() {
        if (!isShowing()) {
            return;
        }
        try {
            // Update WindowManager display
            mWindowManager.updateViewLayout(mDecorView, mWindowParams);
            if (mOnWindowLifecycle == null) {
                return;
            }
            mOnWindowLifecycle.onWindowUpdate(this);
        } catch (IllegalArgumentException e) {
            // If WindowManager has disappeared, calling this will cause a crash
            e.printStackTrace();
        }
    }

    /**
     * Release and recycle
     */
    public void recycle() {
        if (isShowing()) {
            cancel();
        }
        if (mScreenOrientationMonitor != null) {
            mScreenOrientationMonitor.unregisterCallback(mContext);
        }
        if (mOnWindowLifecycle != null) {
            mOnWindowLifecycle.onWindowRecycle(this);
        }
        // Unregister Activity lifecycle
        if (mActivityWindowLifecycle != null) {
            mActivityWindowLifecycle.unregister();
        }
        mOnWindowLifecycle = null;
        mContext = null;
        mDecorView = null;
        mWindowManager = null;
        mWindowParams = null;
        mActivityWindowLifecycle = null;
        mDraggable = null;
        mScreenOrientationMonitor = null;
        // Remove the current instance from the static collection
        sWindowInstanceSet.remove(this);
    }

    /**
     * Get window visibility
     */
    public int getWindowVisibility() {
        return mDecorView.getVisibility();
    }

    /**
     * Set whether the window is visible
     */
    public void setWindowVisibility(int visibility) {
        if (getWindowVisibility() == visibility) {
            return;
        }
        mDecorView.setVisibility(visibility);
        if (mOnWindowLifecycle != null) {
            mOnWindowLifecycle.onWindowVisibilityChanged(this, visibility);
        }
    }

    /**
     * Whether it is currently displayed
     */
    public boolean isShowing() {
        return mShowing;
    }

    /**
     * Get WindowManager object (may be null)
     */
    public WindowManager getWindowManager() {
        return mWindowManager;
    }

    /**
     * Get WindowManager parameter set (may be null)
     */
    public WindowManager.LayoutParams getWindowParams() {
        return mWindowParams;
    }

    /**
     * Get the current drag rule object (may be null)
     */
    public BaseDraggable getDraggable() {
        return mDraggable;
    }

    /**
     * Get context object (may be null)
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Get root layout (may be null)
     */
    public View getDecorView() {
        return mDecorView;
    }

    /**
     * Get content layout
     */
    public View getContentView() {
        if (mDecorView.getChildCount() == 0) {
            return null;
        }
        return mDecorView.getChildAt(0);
    }

    /**
     * Get the width of the current window View
     */
    public int getViewWidth() {
        return getDecorView().getWidth();
    }

    /**
     * Get the height of the current window View
     */
    public int getViewHeight() {
        return getDecorView().getHeight();
    }

    /**
     * Get View by ViewId
     */
    public <V extends View> V findViewById(int id) {
        return mDecorView.findViewById(id);
    }

    /**
     * Jump to Activity
     */
    public void startActivity(Class<? extends Activity> clazz) {
        startActivity(new Intent(mContext, clazz));
    }

    public void startActivity(Intent intent) {
        if (!(mContext instanceof Activity)) {
            // If the current context is not an Activity, calling startActivity must add the new task stack flag
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        mContext.startActivity(intent);
    }

    /**
     * Set visibility
     */
    public X setVisibility(int id, int visibility) {
        findViewById(id).setVisibility(visibility);
        return (X) this;
    }

    /**
     * Set text
     */
    public X setText(int id) {
        return setText(android.R.id.message, id);
    }

    public X setText(int viewId, int stringId) {
        return setText(viewId, mContext.getResources().getString(stringId));
    }

    public X setText(CharSequence text) {
        return setText(android.R.id.message, text);
    }

    public X setText(int id, CharSequence text) {
        ((TextView) findViewById(id)).setText(text);
        return (X) this;
    }

    /**
     * Set text color
     */
    public X setTextColor(int id, int color) {
        ((TextView) findViewById(id)).setTextColor(color);
        return (X) this;
    }

    /**
     * Set text size
     */
    public X setTextSize(int id, float size) {
        ((TextView) findViewById(id)).setTextSize(size);
        return (X) this;
    }

    public X setTextSize(int id, int unit, float size) {
        ((TextView) findViewById(id)).setTextSize(unit, size);
        return (X) this;
    }

    /**
     * Set hint
     */
    public X setHint(int viewId, int stringId) {
        return setHint(viewId, mContext.getResources().getString(stringId));
    }

    public X setHint(int id, CharSequence text) {
        ((TextView) findViewById(id)).setHint(text);
        return (X) this;
    }

    /**
     * Set hint text color
     */
    public X setHintColor(int id, int color) {
        ((TextView) findViewById(id)).setHintTextColor(color);
        return (X) this;
    }

    /**
     * Set background
     */
    public X setBackground(int viewId, int drawableId) {
        Drawable drawable;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawable = mContext.getDrawable(drawableId);
        } else {
            drawable = mContext.getResources().getDrawable(drawableId);
        }
        return setBackground(viewId, drawable);
    }

    public X setBackground(int id, Drawable drawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            findViewById(id).setBackground(drawable);
        } else {
            findViewById(id).setBackgroundDrawable(drawable);
        }
        return (X) this;
    }

    /**
     * Set image
     */
    public X setImageDrawable(int viewId, int drawableId) {
        Drawable drawable;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawable = mContext.getDrawable(drawableId);
        } else {
            drawable = mContext.getResources().getDrawable(drawableId);
        }
        return setImageDrawable(viewId, drawable);
    }

    public X setImageDrawable(int viewId, Drawable drawable) {
        ((ImageView) findViewById(viewId)).setImageDrawable(drawable);
        return (X) this;
    }

    public Handler getHandler() {
        return HANDLER;
    }

    public String getTag() {
        return mTag;
    }

    /**
     * Post with delay
     */
    public boolean post(Runnable runnable) {
        return postDelayed(runnable, 0);
    }

    /**
     * Post with delay for a period of time
     */
    public boolean postDelayed(Runnable runnable, long delayMillis) {
        if (delayMillis < 0) {
            delayMillis = 0;
        }
        return postAtTime(runnable, SystemClock.uptimeMillis() + delayMillis);
    }

    /**
     * Post at a specified time
     */
    public boolean postAtTime(Runnable runnable, long uptimeMillis) {
        // Send message callback related to this WindowManager
        return HANDLER.postAtTime(runnable, this, uptimeMillis);
    }

    /**
     * Remove message callback
     */
    public void removeCallbacks(Runnable runnable) {
        HANDLER.removeCallbacks(runnable);
    }

    public void removeCallbacksAndMessages() {
        HANDLER.removeCallbacksAndMessages(this);
    }

    /**
     * Set click event
     */
    public X setOnClickListener(OnClickListener<? extends View> listener) {
        return setOnClickListener(mDecorView, listener);
    }

    public X setOnClickListener(int id, OnClickListener<? extends View> listener) {
        return setOnClickListener(findViewById(id), listener);
    }

    private X setOnClickListener(View view, EasyWindow.OnClickListener<? extends View> listener) {
        // If currently set as not touchable, remove this flag
        removeWindowFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        view.setClickable(true);
        view.setOnClickListener(new ViewClickWrapper(this, listener));
        return (X) this;
    }

    /**
     * Set long click event
     */
    public X setOnLongClickListener(OnLongClickListener<? extends View> listener) {
        return setOnLongClickListener(mDecorView, listener);
    }

    public X setOnLongClickListener(int id, OnLongClickListener<? extends View> listener) {
        return setOnLongClickListener(findViewById(id), listener);
    }

    private X setOnLongClickListener(View view, EasyWindow.OnLongClickListener<? extends View> listener) {
        // If currently set as not touchable, remove this flag
        removeWindowFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        view.setClickable(true);
        view.setOnLongClickListener(new ViewLongClickWrapper(this, listener));
        return (X) this;
    }

    /**
     * Set touch event
     */
    public X setOnTouchListener(OnTouchListener<? extends View> listener) {
        return setOnTouchListener(mDecorView, listener);
    }

    public X setOnTouchListener(int id, OnTouchListener<? extends View> listener) {
        return setOnTouchListener(findViewById(id), listener);
    }

    private X setOnTouchListener(View view, EasyWindow.OnTouchListener<? extends View> listener) {
        // If currently set as not touchable, remove this flag
        removeWindowFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

        view.setEnabled(true);
        view.setOnTouchListener(new ViewTouchWrapper(this, listener));
        return (X) this;
    }

    /**
     * Click callback
     * @param easyWindow The EasyWindow instance
     * @param view The clicked view
     */
    @Override
    public void run() {
        cancel();
    }

    /**
     * {@link ScreenOrientationMonitor.OnScreenOrientationCallback}
     */
    @Override
    public void onScreenOrientationChange(int newOrientation) {
        if (!isShowing()) {
            return;
        }
        if (mDraggable == null) {
            return;
        }
        mDraggable.onScreenOrientationChange();
    }

    /**
     * Listener for view click events.
     * @param <V> the type of view
     */
    public interface OnClickListener<V extends View> {
        /**
         * Called when the view is clicked.
         * @param easyWindow the EasyWindow instance
         * @param view the clicked view
         */
        void onClick(EasyWindow<?> easyWindow, V view);
    }

    /**
     * Listener for view long click events.
     * @param <V> the type of view
     */
    public interface OnLongClickListener<V extends View> {
        /**
         * Called when the view is long clicked.
         * @param easyWindow the EasyWindow instance
         * @param view the long-clicked view
         * @return true if the callback consumed the long click, false otherwise
         */
        boolean onLongClick(EasyWindow<?> easyWindow, V view);
    }

    /**
     * Listener for view touch events.
     * @param <V> the type of view
     */
    public interface OnTouchListener<V extends View> {
        /**
         * Called when the view is touched.
         * @param easyWindow the EasyWindow instance
         * @param view the touched view
         * @param event the MotionEvent
         * @return true if the callback consumed the touch event, false otherwise
         */
        boolean onTouch(EasyWindow<?> easyWindow, V view, MotionEvent event);
    }

    /**
     * Listener for window lifecycle events.
     */
    public interface OnWindowLifecycle {
        /**
         * Called when the window is shown.
         * @param easyWindow the EasyWindow instance
         */
        default void onWindowShow(EasyWindow<?> easyWindow) {}
        /**
         * Called when the window is updated.
         * @param easyWindow the EasyWindow instance
         */
        default void onWindowUpdate(EasyWindow<?> easyWindow) {}
        /**
         * Called when the window is canceled.
         * @param easyWindow the EasyWindow instance
         */
        default void onWindowCancel(EasyWindow<?> easyWindow) {}
        /**
         * Called when the window is recycled.
         * @param easyWindow the EasyWindow instance
         */
        default void onWindowRecycle(EasyWindow<?> easyWindow) {}
        /**
         * Called when the window visibility changes.
         * @param easyWindow the EasyWindow instance
         * @param visibility the new visibility
         */
        default void onWindowVisibilityChanged(EasyWindow<?> easyWindow, int visibility) {}
    }
}