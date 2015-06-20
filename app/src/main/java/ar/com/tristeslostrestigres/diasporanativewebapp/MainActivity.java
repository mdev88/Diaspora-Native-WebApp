/*
    This file is part of the Diaspora Native WebApp.

    Diaspora Native WebApp is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Diaspora Native WebApp is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the Diaspora Native WebApp.

    If not, see <http://www.gnu.org/licenses/>.
 */

package ar.com.tristeslostrestigres.diasporanativewebapp;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;


public class MainActivity extends ActionBarActivity {
    private WebView webView;
    private static final String TAG = "Diaspora Main";
    private ProgressDialog progressBar;
    private String podDomain;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = new ProgressDialog(MainActivity.this);
        progressBar.setCancelable(true);
        progressBar.setTitle("Please Wait");
        progressBar.setMessage("Loading...");
        progressBar.setMax(50);  // A little cheat to make things appear to load a bit faster ;)
        progressBar.show();

        SharedPreferences config = getSharedPreferences("PodSettings", MODE_PRIVATE);
        podDomain = config.getString("podDomain", null);

        webView = (WebView)findViewById(R.id.webView);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);

        WebSettings wSettings = webView.getSettings();
        wSettings.setJavaScriptEnabled(true);
        wSettings.setBuiltInZoomControls(true);
        if (android.os.Build.VERSION.SDK_INT >= 21)
            wSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        WebViewClient wc = new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, url);
                if (!url.contains(podDomain)) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(i);
                    return true;
                } else {
                    if (!progressBar.isShowing())progressBar.show();
                    return false;
                }
            }

            public void onPageFinished(WebView view, String url) {
                Log.i(TAG, "Finished loading URL: " + url);
                if (progressBar.isShowing()) {
                    progressBar.dismiss();
                }
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e(TAG, "Error: " + description);
                if (progressBar.isShowing()) {
                    progressBar.dismiss();
                }

                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(description)
                        .setPositiveButton("CLOSE", null)
                        .show();
            }
        };

        // This fixes the inability to reshare posts.
        // This solution was taken from the Diaspora WebClient by Terkel Sørensen.
        // Source: https://github.com/voidcode/Diaspora-Webclient/blob/master/src/com/voidcode/diasporawebclient/MainActivity.java
        webView.setWebChromeClient(new WebChromeClient() {
            public boolean onJsAlert(WebView view, String url, String message, JsResult result)
            {
                return super.onJsAlert(view, url, message, result);
            }
        });

        webView.setWebViewClient(wc);
        if (savedInstanceState == null)
            webView.loadUrl("https://"+podDomain);

    }


    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        if (webView.getUrl().contains(podDomain + "/stream") ||
            webView.getUrl().contains(podDomain + "/users/sign_in") ||
            webView.getUrl().equals("https://" + podDomain)) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage("Are you sure you want to exit?")
                    .setPositiveButton("YES", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton("NO", null)
                    .show();
        } else {
            webView.goBack();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.reload) {
            progressBar.show();
            webView.reload();
            return true;
        }

        if (id == R.id.liked) {
            progressBar.show();
            webView.loadUrl("https://" + podDomain + "/liked");
            return true;
        }

        if (id == R.id.commented) {
            progressBar.show();
            webView.loadUrl("https://"+podDomain+"/commented");
            return true;
        }

        if (id == R.id.followed_tags) {
            progressBar.show();
            webView.loadUrl("https://" + podDomain + "/followed_tags");
            return true;
        }

        if (id == R.id.clearCookies) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Confirmation")
                    .setMessage("Clearing the cookies will log you out and clear all session data. Do you want to proceed?")
                    .setPositiveButton("YES",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    progressBar.show();
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        CookieManager.getInstance().removeAllCookies(null);
                                        CookieManager.getInstance().removeSessionCookies(null);
                                    }
                                    else {
                                        CookieManager.getInstance().removeAllCookie();
                                        CookieManager.getInstance().removeSessionCookie();
                                    }
                                    webView.reload();
                                    dialog.cancel();
                                }
                            })
                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    }).show();
            return true;
        }

        if (id == R.id.changePod) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Confirmation")
                    .setMessage("This will erase all cookies and session data. Do you really want to change pods?")
                    .setPositiveButton("YES",
                            new DialogInterface.OnClickListener() {
                                @TargetApi(11)
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    Intent i = new Intent(MainActivity.this, PodsActivity.class);
                                    startActivity(i);
                                    finish();
                                }
                            })
                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @TargetApi(11)
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    }).show();
            return true;
        }

        if (id == R.id.exit_app) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage("Are you sure you want to exit?")
                    .setPositiveButton("YES", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton("NO", null)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
