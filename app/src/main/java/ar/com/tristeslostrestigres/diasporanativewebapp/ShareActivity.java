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
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

import ar.com.tristeslostrestigres.diasporanativewebapp.utils.Helpers;


public class ShareActivity extends MainActivity {

    private WebView webView;
    private static final String TAG = "Diaspora Share";
    private String podDomain;
    private ProgressDialog progressDialog;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(true);
        progressDialog.setTitle(getString(R.string.please_wait));
        progressDialog.setMessage(getString(R.string.loading));

        SharedPreferences config = getSharedPreferences("PodSettings", MODE_PRIVATE);
        podDomain = config.getString("podDomain", null);

        webView = (WebView)findViewById(R.id.webView);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);

        WebSettings wSettings = webView.getSettings();
        wSettings.setJavaScriptEnabled(true);
        wSettings.setBuiltInZoomControls(true);

        if (Build.VERSION.SDK_INT >= 21)
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
                if (progressDialog.isShowing()) progressDialog.dismiss();
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e(TAG, "Error: " + description);
                if (progressDialog.isShowing()) progressDialog.dismiss();

                new AlertDialog.Builder(ShareActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(description)
                        .setPositiveButton("CLOSE", null)
                        .show();
            }
        };

        webView.setWebChromeClient(new WebChromeClient() {
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return super.onJsAlert(view, url, message, result);
            }
        });

        webView.setWebViewClient(wc);

        if (savedInstanceState == null) {
            if (Helpers.isOnline(ShareActivity.this)) {

//                if (!progressDialog.isShowing()) progressDialog.show();
                webView.loadUrl("https://"+podDomain+"/status_messages/new");

            } else {  // No Internet connection
                Toast.makeText(
                        ShareActivity.this,
                        getString(R.string.no_internet),
                        Toast.LENGTH_LONG).show();
            }
        }


        // This block of code was taken from the Diaspora WebClient by Terkel Sørensen
        // and was modified slightly by me.
        // Original source: https://github.com/voidcode/Diaspora-Webclient/blob/master/src/com/voidcode/diasporawebclient/ShareActivity.java
        Intent intent = getIntent();
        final Bundle extras = intent.getExtras();
        String action = intent.getAction();

        if (Intent.ACTION_SEND.equals(action)) {
            webView.setWebViewClient(new WebViewClient() {

                public void onPageFinished(WebView view, String url) {

                    if (progressDialog.isShowing()) progressDialog.dismiss();

                    if (extras.containsKey(Intent.EXTRA_TEXT) && extras.containsKey(Intent.EXTRA_SUBJECT)) {
                        final String extraText = (String) extras.get(Intent.EXTRA_TEXT);
                        final String extraSubject = (String) extras.get(Intent.EXTRA_SUBJECT);

                        webView.setWebViewClient(new WebViewClient() {
                            @Override
                            public boolean shouldOverrideUrlLoading(WebView view, String url) {

                                finish();

                                Toast.makeText(ShareActivity.this, "Please reload the stream", Toast.LENGTH_SHORT).show();

                                Intent i = new Intent(ShareActivity.this, MainActivity.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                i.putExtra("fromShare", true);
                                startActivity(i);

                                return false;
                            }
                        });

                        webView.loadUrl("javascript:(function() { " +
                                "document.getElementsByTagName('textarea')[0].style.height='110px'; " +
                                "document.getElementsByTagName('textarea')[0].innerHTML = '[" + extraSubject + "](" + extraText + ") #ViaDiasporaNativeWebApp'; " +
                                "    if(document.getElementById(\"main_nav\")) {" +
                                "        document.getElementById(\"main_nav\").parentNode.removeChild(" +
                                "        document.getElementById(\"main_nav\"));" +
                                "    } else if (document.getElementById(\"main-nav\")) {" +
                                "        document.getElementById(\"main-nav\").parentNode.removeChild(" +
                                "        document.getElementById(\"main-nav\"));" +
                                "    }" +
                                "})();");
                    }
                }
            });
        }


    }

    @Override
    protected void onDestroy() {
        if (progressDialog.isShowing()) progressDialog.dismiss();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_compose, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.reload) {
            if (Helpers.isOnline(ShareActivity.this)) {
                if (!progressDialog.isShowing()) progressDialog.show();
                webView.reload();
                return true;
            } else {  // No Internet connection
                Toast.makeText(
                        ShareActivity.this,
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
