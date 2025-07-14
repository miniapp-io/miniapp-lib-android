package com.hjq.window1;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;

/**
 *    author : Android Wheel Brother
 *    github : https://github.com/getActivity/EasyWindow
 *    time   : 2019/01/04
 *    desc   : Floating window lifecycle management, preventing memory leaks
 */
final class ActivityWindowLifecycle implements Application.ActivityLifecycleCallbacks {

    private Activity mActivity;
    private EasyWindow<?> mEasyWindow;

    ActivityWindowLifecycle(EasyWindow<?> easyWindow, Activity activity) {
        mActivity = activity;
        mEasyWindow = easyWindow;
    }

    /**
     * Register listener
     */
    void register() {
        if (mActivity == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mActivity.registerActivityLifecycleCallbacks(this);
        } else {
            mActivity.getApplication().registerActivityLifecycleCallbacks(this);
        }
    }

    /**
     * Unregister listener
     */
    void unregister() {
        if (mActivity == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mActivity.unregisterActivityLifecycleCallbacks(this);
        } else {
            mActivity.getApplication().unregisterActivityLifecycleCallbacks(this);
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {}

    @Override
    public void onActivityResumed(Activity activity) {}

    @Override
    public void onActivityPaused(Activity activity) {
        // Be sure to destroy in the onPaused method. If you do it in onDestroyed, there is still a chance of memory leak.
        if (mActivity != activity || !mActivity.isFinishing() || mEasyWindow == null || !mEasyWindow.isShowing()) {
            return;
        }
        mEasyWindow.cancel();
    }

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (mActivity != activity) {
            return;
        }
        // Release the reference to Activity
        mActivity = null;

        if (mEasyWindow == null) {
            return;
        }
        mEasyWindow.recycle();
        mEasyWindow = null;
    }
}