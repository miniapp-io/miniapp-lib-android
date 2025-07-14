package com.hjq.window1;

import android.view.View;

/**
 *    author : Android Wheel Brother
 *    github : https://github.com/getActivity/EasyWindow
 *    time   : 2021/09/03
 *    desc   : {@link View.OnLongClickListener} wrapper class
 */
@SuppressWarnings("rawtypes")
final class ViewLongClickWrapper implements View.OnLongClickListener {

    private final EasyWindow<?> mEasyWindow;
    private final EasyWindow.OnLongClickListener mListener;

    ViewLongClickWrapper(EasyWindow<?> easyWindow, EasyWindow.OnLongClickListener listener) {
        mEasyWindow = easyWindow;
        mListener = listener;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean onLongClick(View view) {
        if (mListener == null) {
            return false;
        }
        return mListener.onLongClick(mEasyWindow, view);
    }
}