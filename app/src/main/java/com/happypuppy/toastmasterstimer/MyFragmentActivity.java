package com.happypuppy.toastmasterstimer;


import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Calendar;


public class MyFragmentActivity extends FragmentActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new PrefFragment()).commit();
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_dark_mode", true)) {
            setTheme(R.style.AppTheme);
        } else {
            setTheme(R.style.AppThemeLight);
        }
    }

    public static class PrefFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.user_settings, rootKey);

            Preference preference = findPreference("about_key");
            preference.setSummary("version " + BuildConfig.VERSION_NAME + ", " + Calendar.getInstance().get(Calendar.YEAR));

            findPreference("pref_dark_mode").setOnPreferenceChangeListener((preference1, newValue) -> {
                if (newValue instanceof Boolean) {
                    boolean isDarkMode = Boolean.parseBoolean(newValue.toString());
                    if (isDarkMode) {
                        getActivity().setTheme(R.style.AppTheme);
                    } else {
                        getActivity().setTheme(R.style.AppThemeLight);
                    }
                    getActivity().recreate();
                }
                return true;
            });
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

        }
    }
}


