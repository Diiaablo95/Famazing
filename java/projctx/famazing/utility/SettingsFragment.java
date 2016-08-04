package projctx.famazing.utility;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import java.util.Locale;

import projctx.famazing.R;

/**
 * Fragment used to give user possibility to modify some application details.
 */
public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
        final EditTextPreference alertNumberPreference = (EditTextPreference) findPreference(getString(R.string.emergency_number_key));
        alertNumberPreference.setSummary(String.format(Locale.ENGLISH, "ACTUAL VALUE: %s", alertNumberPreference.getText()));

        alertNumberPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String newNumber = (String) newValue;
                boolean shouldUpdate = true;
                //If the string doesn't contain only number (which should be impossible since keyboard provided is formed by only numbers)
                if (!newNumber.matches("[0-9]+")) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Wrong number format.")
                            .setMessage("Insert a correct number to call. Only digits are allowed.")
                            .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).show();
                    shouldUpdate = false;
                } else {
                    preference.setSummary(String.format(Locale.ENGLISH, "ACTUAL VALUE %s", newNumber));
                }
                return shouldUpdate;
            }
        });
    }
}
