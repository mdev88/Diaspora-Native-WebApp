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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

import ar.com.tristeslostrestigres.diasporanativewebapp.services.GetPodsService;
import ar.com.tristeslostrestigres.diasporanativewebapp.utils.Helpers;


public class PodsActivity extends ActionBarActivity {

    BroadcastReceiver br;
    EditText filter;
    ListView lv;
    ImageView imgSelectPod;
    ProgressDialog ringProgressDialog;
    private static final String TAG = "Diaspora Pods";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pods);

        filter = (EditText) findViewById(R.id.edtFilter);
        lv = (ListView) findViewById(R.id.lstPods);
        lv.setTextFilterEnabled(true);

        imgSelectPod = (ImageView) findViewById(R.id.imgSelectPod);
        imgSelectPod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askConfirmation(filter.getText().toString());
            }
        });

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                String[] pods = extras.getStringArray("pods");

                if (ringProgressDialog != null)
                    ringProgressDialog.dismiss();

                if (pods != null && pods.length>0)
                    updateListview(pods);
                else {
                    Log.d(TAG, "Could not retrieve list of pods");
                    Toast.makeText(
                            PodsActivity.this,
                            "Error: Could not retrieve list of pods!",
                            Toast.LENGTH_LONG).show();
                }

            }
        };

        registerReceiver(br, new IntentFilter(GetPodsService.MESSAGE));

        if (Helpers.isOnline(PodsActivity.this)) {
            ringProgressDialog =
                    ProgressDialog.show(PodsActivity.this, null, "Loading pod list ...", true);
            ringProgressDialog.setCancelable(false);

            Intent i= new Intent(PodsActivity.this, GetPodsService.class);
            startService(i);
        } else {
            Toast.makeText(
                    PodsActivity.this,
                    "Sorry, you must be connected to the Internet to proceed",
                    Toast.LENGTH_LONG).show();
        }


    }

    private void updateListview(String[] source) {
        final ArrayList<String> podList = new ArrayList<>();

        for (int i = 0 ; i < source.length ; i++) {
            podList.add(source[i].toLowerCase());
        }
        Collections.sort(podList);

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                PodsActivity.this,
                android.R.layout.simple_list_item_1,
                podList);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                askConfirmation(((TextView) view).getText().toString());
            }
        });

        adapter.getFilter().filter(filter.getText());
        filter.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                (adapter).getFilter().filter(s.toString());

                if (s.toString().length()>3 && s.toString().contains("."))
                    imgSelectPod.setVisibility(View.VISIBLE);
                else
                    imgSelectPod.setVisibility(View.GONE);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    public void askConfirmation(final String podDomain) {
        if (Helpers.isOnline(PodsActivity.this)) {

            // Show a warning when is connected using mobile Internet
            if (Helpers.isUsingMobile(PodsActivity.this)) {
                Toast.makeText(
                        PodsActivity.this,
                        "Warning: Connected via mobile",
                        Toast.LENGTH_SHORT).show();
            }

            new AlertDialog.Builder(PodsActivity.this)
                    .setTitle("Confirmation")
                    .setMessage("Do you want to use the pod: "+podDomain+"?")
                    .setPositiveButton("YES",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                    SharedPreferences sp = getSharedPreferences("PodSettings", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sp.edit();
                                    editor.putString("podDomain", podDomain);
                                    editor.apply();

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        CookieManager.getInstance().removeAllCookies(null);
                                        CookieManager.getInstance().removeSessionCookies(null);
                                    } else {
                                        CookieManager.getInstance().removeAllCookie();
                                        CookieManager.getInstance().removeSessionCookie();
                                    }

                                    Intent i = new Intent(PodsActivity.this, MainActivity.class);
                                    dialog.cancel();
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

        } else { // No Internet connection
            Toast.makeText(
                    PodsActivity.this,
                    "Sorry, you must be connected to the Internet to proceed",
                    Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onBackPressed() {
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
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(br);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pods, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.reload) {
            if (Helpers.isOnline(PodsActivity.this)) {

                // Show a warning when is connected using mobile Internet
                if (Helpers.isUsingMobile(PodsActivity.this)) {
                    Toast.makeText(
                            PodsActivity.this,
                            "Warning: Connected via mobile",
                            Toast.LENGTH_SHORT).show();
                }

                ringProgressDialog.show();
                Intent i= new Intent(PodsActivity.this, GetPodsService.class);
                startService(i);
                return true;
            } else {  // No Internet connection
                Toast.makeText(
                        PodsActivity.this,
                        "Sorry, you must be connected to the Internet to proceed",
                        Toast.LENGTH_LONG).show();
                return false;
            }

        }

        return super.onOptionsItemSelected(item);
    }
}
