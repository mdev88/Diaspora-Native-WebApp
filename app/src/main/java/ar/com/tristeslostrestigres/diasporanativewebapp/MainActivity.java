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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.Toast;

import ar.com.tristeslostrestigres.diasporanativewebapp.receivers.NetworkChangeReceiver;
import ar.com.tristeslostrestigres.diasporanativewebapp.utils.Helpers;


public class MainActivity extends ActionBarActivity {
    
    private WebView webView;
    private static final String TAG = "Diaspora Main";
    private ProgressDialog progressDialog;
    private String podDomain;
    private BroadcastReceiver networkStateReceiver;
    private boolean networkStateReceiverIsRegistered;


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        regNetworkStateChangeReceiver();

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(true);
        progressDialog.setTitle(getString(R.string.please_wait));
        progressDialog.setMessage(getString(R.string.loading));
        progressDialog.setMax(50);  // A little cheat to make things appear to load a bit faster ;)

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
                    if (!progressDialog.isShowing()) progressDialog.show();
                    return false;
                }
            }

            public void onPageFinished(WebView view, String url) {
                Log.i(TAG, "Finished loading URL: " + url);
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e(TAG, "Error: " + description);
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
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
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return super.onJsAlert(view, url, message, result);
            }
        });

        webView.setWebViewClient(wc);
        if (savedInstanceState == null) {
            if (Helpers.isOnline(MainActivity.this)) {
                progressDialog.show();
                webView.loadUrl("https://"+podDomain);
            } else {  // No Internet connection
                Toast.makeText(
                        MainActivity.this,
                        getString(R.string.no_internet),
                        Toast.LENGTH_LONG).show();
            }
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkStateReceiverIsRegistered)
            unregisterReceiver(networkStateReceiver);
    }

    @Override
    public void onBackPressed() {
        if (webView.getUrl().contains(podDomain + "/stream") ||
            webView.getUrl().contains(podDomain + "/users/sign_in") ||
            webView.getUrl().equals("https://" + podDomain) ||
            webView.getUrl().equals(""))  {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(getString(R.string.confirm_exit))
                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                            .setNegativeButton(getString(R.string.no), null)
                            .show();
        } else {
            webView.goBack();
        }
    }


    private void regNetworkStateChangeReceiver() {
        networkStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                if (extras.getString(NetworkChangeReceiver.CONNECTION_STATE_CHANGE).equals("Wifi enabled") ||
                        extras.getString(NetworkChangeReceiver.CONNECTION_STATE_CHANGE).equals("Mobile data enabled")) {
                    Toast.makeText(
                            MainActivity.this,
                            getString(R.string.connection_established),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(
                            MainActivity.this,
                            getString(R.string.connection_lost),
                            Toast.LENGTH_SHORT).show();
                }
            }
        };
        registerReceiver(networkStateReceiver, new IntentFilter(NetworkChangeReceiver.CONNECTION_STATE_CHANGE));
        networkStateReceiverIsRegistered = true;
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
            if (Helpers.isOnline(MainActivity.this)) {

                if(Helpers.isUsingMobile(MainActivity.this))
                    Helpers.warningMobile(MainActivity.this);

                if (!progressDialog.isShowing()) progressDialog.show();
                webView.reload();
                return true;
            } else {  // No Internet connection
                Toast.makeText(
                        MainActivity.this,
                        getString(R.string.no_internet),
                        Toast.LENGTH_LONG).show();
                return false;
            }

        }

        if (id == R.id.liked) {
            if (Helpers.isOnline(MainActivity.this)) {
                progressDialog.show();
                webView.loadUrl("https://" + podDomain + "/liked");
                return true;
            } else {  // No Internet connection
                Toast.makeText(
                        MainActivity.this,
                        getString(R.string.no_internet),
                        Toast.LENGTH_LONG).show();
                return false;
            }
        }

        if (id == R.id.commented) {
            if (Helpers.isOnline(MainActivity.this)) {
                progressDialog.show();
                webView.loadUrl("https://"+podDomain+"/commented");
                return true;
            } else {  // No Internet connection
                Toast.makeText(
                        MainActivity.this,
                        getString(R.string.no_internet),
                        Toast.LENGTH_LONG).show();
                return false;
            }
        }

        if (id == R.id.followed_tags) {
            if (Helpers.isOnline(MainActivity.this)) {
                progressDialog.show();
                webView.loadUrl("https://" + podDomain + "/followed_tags");
                return true;
            } else {  // No Internet connection
                Toast.makeText(
                        MainActivity.this,
                        getString(R.string.no_internet),
                        Toast.LENGTH_LONG).show();
                return false;
            }
        }

        if (id == R.id.clearCookies) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(getString(R.string.confirmation))
                    .setMessage(getString(R.string.clear_cookies_warning))
                    .setPositiveButton(getString(R.string.yes),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    progressDialog.show();
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
                    .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    }).show();
            return true;
        }

        if (id == R.id.changePod) {

            if (Helpers.isOnline(MainActivity.this)) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(getString(R.string.confirmation))
                        .setMessage(getString(R.string.change_pod_warning))
                        .setPositiveButton(getString(R.string.yes),
                                new DialogInterface.OnClickListener() {
                                    @TargetApi(11)
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                        Intent i = new Intent(MainActivity.this, PodsActivity.class);
                                        startActivity(i);
                                        finish();
                                    }
                                })
                        .setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
                            @TargetApi(11)
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).show();
                return true;
            } else {  // No Internet connection
                Toast.makeText(
                        MainActivity.this,
                        getString(R.string.no_internet),
                        Toast.LENGTH_LONG).show();
                return false;
            }

        }

        if (id == R.id.exit_app) {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(getString(R.string.confirm_exit))
                    .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(getString(R.string.no), null)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
