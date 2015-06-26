package ar.com.tristeslostrestigres.diasporanativewebapp.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ar.com.tristeslostrestigres.diasporanativewebapp.utils.NetworkUtil;

public class NetworkChangeReceiver extends BroadcastReceiver {
    public static final String CONNECTION_STATE_CHANGE = "CONNECTION_STATE_CHANGE";

    public NetworkChangeReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String status = NetworkUtil.getConnectivityStatusString(context);

        Intent broadcastIntent = new Intent(CONNECTION_STATE_CHANGE);
        broadcastIntent.putExtra("CONNECTION_STATE_CHANGE", status);
        context.sendBroadcast(broadcastIntent);

    }
}
