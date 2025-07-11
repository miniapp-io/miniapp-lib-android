package com.hjq.window1;

import android.view.View;

/**
 *    author : Android Wheel Brother
 *    github : https://github.com/getActivity/EasyWindow
 *    time   : 2019/01/04
 *    desc   : {@link View.OnClickListener} wrapper class
 */
@SuppressWarnings("rawtypes")
final class ViewClickWrapper implements View.OnClickListener {

    private final EasyWindow<?> mEasyWindow;
    private final EasyWindow.OnClickListener mListener;

    ViewClickWrapper(EasyWindow<?> easyWindow, EasyWindow.OnClickListener listener) {
        mEasyWindow = easyWindow;
        mListener = listener;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onClick(View view) {
        if (mListener == null) {
            return;
        }
        mListener.onClick(mEasyWindow, view);
    }
}