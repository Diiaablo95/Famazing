package projctx.famazing.ui;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import projctx.famazing.data.Family;

/**
 * Class with knowledge of what has to be written in the cookie to ensure normal behaviour of the application.
 */
public abstract class CookieActivity extends AppCompatActivity {

    public static final String USER_ID_KEY = "user_id";
    public static final String USER_NAME_KEY = "user_name";
    public static final String USER_MEMBERSHIP_KEY = "user_membership";
    public static final String USER_FAMILY_KEY = "user_family";
    public static final String EMERGENCY_NUMBER_KEY = "emergency_number";

    protected void writeCookie(int userId, String userName, Family.Membership userMembership, int familyId) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putInt(USER_ID_KEY, userId);
        editor.putString(USER_NAME_KEY, userName);
        editor.putString(USER_MEMBERSHIP_KEY, userMembership.toString());
        editor.putInt(USER_FAMILY_KEY, familyId);
        editor.apply();
    }

}
