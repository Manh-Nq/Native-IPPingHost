package com.tapi.ippingdemo;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class PrivacyWebViewActivity extends Activity {
    /* access modifiers changed from: protected */
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(C0565R.layout.activity_privacy_web_view);
        ((WebView) findViewById(C0565R.C0567id.webview)).loadUrl("http://lipinic.com/ping/privacy.html");
    }
}