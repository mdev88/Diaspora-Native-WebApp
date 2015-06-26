package ar.com.tristeslostrestigres.diasporanativewebapp.utils;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class Helpers {

    public static boolean isOnline(Context context){
        ConnectivityManager cnm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cnm.getActiveNetworkInfo();
        return ni != null && ni.isConnectedOrConnecting();
    }

    public static boolean isUsingMobile(Context context) {
        ConnectivityManager cnm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cnm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return ni != null && ni.isConnectedOrConnecting();
    }

    // Show a warning when is connected using mobile Internet
    public static void warningMobile(Context ctx) {
        if (Helpers.isUsingMobile(ctx)) {
            Toast.makeText(
                    ctx,
                    "Warning: Connected via mobile",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
