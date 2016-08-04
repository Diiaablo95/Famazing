package projctx.famazing.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;

import projctx.famazing.utility.AlertService;
import projctx.famazing.utility.PositionService;

/**
 * First activity created by the application which checks which activity to show next based on the whether the cookie is present or not.
 */
public class CheckActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Starts the two services everytime a user logs in.
        if (!PositionService.running) {
            Intent positionServiceIntent = new Intent(this, PositionService.class);
            startService(positionServiceIntent);
        }
        if (!AlertService.running) {
            Intent alertServiceIntent = new Intent(this, AlertService.class);
            startService(alertServiceIntent);
        }

        Intent intent;
        if (isCookie()) {
            intent = new Intent(this, HomeActivity.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private boolean isCookie() {
        return PreferenceManager.getDefaultSharedPreferences(this).getInt(CookieActivity.USER_ID_KEY, -1) != -1;
    }
}
