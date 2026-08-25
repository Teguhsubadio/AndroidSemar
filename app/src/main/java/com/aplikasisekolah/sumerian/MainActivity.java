package com.aplikasisekolah.sumerian;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {
    private WebView mWebView;
    private LinearLayout mErrorLayout;
    private static final String TARGET_SSID = "Semar Webserver";
    private static final int PERMISSION_REQUEST_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWebView = (WebView) findViewById(R.id.webview);
        mErrorLayout = (LinearLayout) findViewById(R.id.error_layout);
        Button retryButton = (Button) findViewById(R.id.retry_button);
        
        // Ensure webview can receive focus for clicks
        mWebView.setFocusable(true);
        mWebView.setFocusableInTouchMode(true);
        
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkNetworkAndLoad()) {
                    mErrorLayout.setVisibility(View.GONE);
                    mWebView.setVisibility(View.VISIBLE);
                }
            }
        });

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        
        mWebView.setWebViewClient(new MyWebViewClient());
        mWebView.setWebChromeClient(new WebChromeClient());
        
        checkNetworkAndLoad();
    }

    private boolean checkNetworkAndLoad() {
        if (hasLocationPermission()) {
            if (isCorrectSSID()) {
                mWebView.setVisibility(View.VISIBLE);
                mErrorLayout.setVisibility(View.GONE);
                mWebView.loadUrl("http://192.168.4.200");
                return true;
            } else {
                mWebView.setVisibility(View.GONE);
                mErrorLayout.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Please connect to Wi-Fi: " + TARGET_SSID, Toast.LENGTH_LONG).show();
                return false;
            }
        } else {
            mWebView.setVisibility(View.GONE);
            mErrorLayout.setVisibility(View.VISIBLE);
            requestLocationPermission();
            return false;
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, PERMISSION_REQUEST_CODE);
    }

    private boolean isCorrectSSID() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info != null) {
            String ssid = info.getSSID();
            if (ssid != null) {
                // Remove quotes from SSID if present
                if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length() - 1);
                }
                return TARGET_SSID.equals(ssid);
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkNetworkAndLoad();
            } else {
                Toast.makeText(this, "Location permission is required to verify network SSID", Toast.LENGTH_LONG).show();
            }
        }
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            // Only show the error screen if the main page fails to load.
            // This prevents sub-resource failures (like a missing icon) from breaking the app.
            if (request.isForMainFrame()) {
                mWebView.setVisibility(View.GONE);
                mErrorLayout.setVisibility(View.VISIBLE);
            }
        }

        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            final Uri uri = Uri.parse(url);
            return handleUri(uri);
        }

        @TargetApi(Build.VERSION_CODES.N)
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            final Uri uri = request.getUrl();
            return handleUri(uri);
        }

        public boolean handleUri(final Uri uri) {
            final String host = uri.getHost();
            // Check for null host to prevent crashes on non-http URIs (like tel: or mailto:)
            if (host != null && host.endsWith("192.168.4.200")) {
                return false;
            } else {
                try {
                    final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    // Fallback if no app can handle the intent
                    return false;
                }
            }
        }
    }
    @Override
    public void onBackPressed() {
        // Back button disabled
    }
}