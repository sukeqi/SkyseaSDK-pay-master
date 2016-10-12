package com.skysea.sdk.main;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.skysea.app.BaseActivity;
import com.skysea.sdk.R;

public class WechatActivity extends BaseActivity {
    private WebView wechat_web;
    private ProgressBar pb;
    private ImageView back;
    String ordernum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wechat);
        initView();

    }

    private void initView() {
        ordernum = getIntent().getStringExtra("ordernum");
        pb = (ProgressBar) findViewById(R.id.pb);
        pb.setMax(100);
        wechat_web = (WebView) findViewById(R.id.wechat_web);
        back = (ImageView) findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WechatActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });

        wechat_web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                pb.setVisibility(View.VISIBLE);
                pb.setProgress(newProgress);
                if (newProgress == 100) {
                    pb.setVisibility(View.GONE);
                }
                super.onProgressChanged(view, newProgress);
            }
        });
        WebSettings settings = wechat_web.getSettings();
        settings.setUseWideViewPort(true);//适配网页跟webview大小一样
        settings.setSupportZoom(true);
        settings.setJavaScriptEnabled(true);
        String url = "http://www.ya247.com/v2_phonegame/SJPay/sfth5wxpay?order=" + ordernum + "";
        wechat_web.loadUrl(url);
        WebViewClient webViewClient = new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("weixin://wap/pay")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri uri = Uri.parse(url);
                    intent.setData(uri);
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(WechatActivity.this, "检测到你的手机没有安装微信", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
        };
        wechat_web.setWebViewClient(webViewClient);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && wechat_web.canGoBack()) {
            Intent intent = new Intent(WechatActivity.this, MainActivity.class);
            startActivity(intent);
        }
        return super.onKeyDown(keyCode, event);
    }
}

