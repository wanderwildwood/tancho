package com.wanderwildwood.einkbirding;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import org.woheller69.preferences.EditTextSwitchPreference;

public class SettingsActivity extends BaseActivity {
Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_settings);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        DestinationsKt.wireDestinations(this, Destination.SETTINGS);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            PreferenceScreen preferenceScreen = getPreferenceScreen();
            Preference writeWav = getPreferenceManager().findPreference("write_wav");
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) preferenceScreen.removePreference(writeWav);

            Preference reset = getPreferenceManager().findPreference("reset");

            if (reset != null) reset.setOnPreferenceClickListener(preference -> {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

                sharedPreferences.edit().remove("audio_source").apply();
                sharedPreferences.edit().remove("high_pass").apply();
                sharedPreferences.edit().remove("model_threshold").apply();
                sharedPreferences.edit().remove("play_sound").apply();
                sharedPreferences.edit().remove("write_wav").apply();
                sharedPreferences.edit().remove("bluetooth").apply();

                onCreatePreferences(savedInstanceState,rootKey);
                return false;
            });

            Preference language = getPreferenceManager().findPreference("language");
            if (language != null) language.setOnPreferenceClickListener(preference -> {
                // Create an intent to open the app's settings
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
                return true; // Return true to indicate that the click event has been handled

            });
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) preferenceScreen.removePreference(language);

            EditTextSwitchPreference manualLocationValue = findPreference("manual_location_value");
            manualLocationValue.setOnPreferenceChangeListener((preference, newValue) -> {
                String newVal = newValue.toString();
                if (isValidGPSFormat(newVal) && isValidGPSRange(newVal)) {
                    return true;
                } else {
                    Toast.makeText(requireContext(),
                            requireContext().getString(R.string.error_invalid_GPS), Toast.LENGTH_SHORT).show();
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
                    sharedPreferences.edit().remove("manual_location_value").apply();
                    manualLocationValue.setText("0.000/0.000");
                    return false;
                }
            });

        }
        private boolean isValidGPSFormat(String value) {
            if (value == null || value.isEmpty()) return false;
            return value.matches("^-?\\d+(\\.\\d+)?/-?\\d+(\\.\\d+)?$");
        }

        private boolean isValidGPSRange(String value) {
            try {
                String[] parts = value.split("/");
                double lat = Double.parseDouble(parts[0]);
                double lon = Double.parseDouble(parts[1]);
                return lat >= -90 && lat <= 90 &&
                        lon >= -180 && lon <= 180;
            } catch (Exception e) {
                return false;
            }
        }
    }
}