package com.skysea.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.webkit.WebView;

/**
 * Created by jyd-pc006 on 16/10/11.
 */

public class ArticleWebview extends WebView {
    private OnLoadFinishListener mOnLoadFinishListener;

    public interface OnLoadFinishListener {
        public void onLoadFinish();
    }

    private boolean isRendered = false;

    public ArticleWebview(Context context) {
        super(context);
        init();
    }

    public ArticleWebview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ArticleWebview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!isRendered) {
            isRendered = getContentHeight() > 0;
            if (mOnLoadFinishListener != null) {
                mOnLoadFinishListener.onLoadFinish();
            }
        }
    }

    public void setOnLoadFinishListener(OnLoadFinishListener mOnLoadFinishListener) {
        this.mOnLoadFinishListener = mOnLoadFinishListener;
    }
}
