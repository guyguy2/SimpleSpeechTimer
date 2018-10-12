package com.happypuppy.toastmasterstimer;


import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;

import java.util.Calendar;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;


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
            //addPreferencesFromResource(R.xml.user_settings);
            setPreferencesFromResource(R.xml.user_settings, rootKey);

            Preference preference = findPreference("about_key");
            preference.setSummary("version " + BuildConfig.VERSION_NAME + ", 2013 - " + Calendar.getInstance().get(Calendar.YEAR));

            findPreference("pref_dark_mode").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue instanceof Boolean) {
                        Boolean isDarkMode = Boolean.valueOf(newValue.toString());
                        if (isDarkMode) {
                            getActivity().setTheme(R.style.AppTheme);
                        } else {
                            getActivity().setTheme(R.style.AppThemeLight);
                        }
                        getActivity().recreate();
                    }
                    return true;
                }
            });
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

        }
    }
}


