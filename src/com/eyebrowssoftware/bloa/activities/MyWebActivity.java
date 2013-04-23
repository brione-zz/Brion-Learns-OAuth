package com.eyebrowssoftware.bloa.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.webkit.WebView;

import com.eyebrowssoftware.bloa.R;

public class MyWebActivity extends FragmentActivity {

    private WebView mWebView;

    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        this.setContentView(R.layout.my_webview);

        mWebView = (WebView) this.findViewById(R.id.web_view);

        Intent intent = this.getIntent();
        Uri uri = intent.getData();
        mWebView.loadUrl(uri.toString());
    }

}
