package com.hjq.window1;

import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.res.Configuration;

/**
 *    author : Android Wheel Brother
 *    github : https://github.com/getActivity/EasyWindow
 *    time   : 2022/10/03
 *    desc   : Screen orientation rotation monitoring
 */
final class ScreenOrientationMonitor implements ComponentCallbacks {

   /** Current screen orientation */
   private int mScreenOrientation;

   /** Screen rotation callback */
   private OnScreenOrientationCallback mCallback;

   public ScreenOrientationMonitor(Configuration configuration) {
      mScreenOrientation = configuration.orientation;
   }

   /**
    * Register listener
    */
   void registerCallback(Context context, OnScreenOrientationCallback callback) {
      context.getApplicationContext().registerComponentCallbacks(this);
      mCallback = callback;
   }

   /**
    * Unregister listener
    */
   void unregisterCallback(Context context) {
      context.getApplicationContext().unregisterComponentCallbacks(this);
      mCallback = null;
   }

   @Override
   public void onConfigurationChanged(Configuration newConfig) {
      if (mScreenOrientation == newConfig.orientation) {
         return;
      }
      mScreenOrientation = newConfig.orientation;

      if (mCallback == null) {
         return;
      }
      mCallback.onScreenOrientationChange(mScreenOrientation);
   }

   @Override
   public void onLowMemory() {
      // default implementation ignored
   }

   /**
    * Screen orientation listener
    */
   interface OnScreenOrientationCallback {

      /**
       * Monitor screen rotation
       *
       * @param newOrientation         Latest screen orientation
       */
      default void onScreenOrientationChange(int newOrientation) {}
   }
}