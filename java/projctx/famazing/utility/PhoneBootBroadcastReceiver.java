package projctx.famazing.utility;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import projctx.famazing.ui.CookieActivity;

/**
 * Broadcast receiver used to catch the event of the phone boot in order to start the service for emergency calls.
 */
public class PhoneBootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.w("TAG", "Phone booted");
        //If there is a user logged into the application, then start the service(s)
        if (PreferenceManager.getDefaultSharedPreferences(context).getInt(CookieActivity.USER_ID_KEY, -1) != -1) {
            Intent alertServiceIntent = new Intent(context, AlertService.class);
            context.startService(alertServiceIntent);
            Log.d("TAG", "Alert service started!");
            Intent positionServiceIntent = new Intent(context, PositionService.class);
            context.startService(positionServiceIntent);
            Log.d("TAG", "Position service started!");
        }
    }
}
