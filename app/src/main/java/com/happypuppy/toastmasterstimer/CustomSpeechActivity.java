package com.happypuppy.toastmasterstimer;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.NavUtils;

import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Toast;

public class CustomSpeechActivity extends Activity {

    private NumberPicker npFrom = null;
    private NumberPicker npTo = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_dark_mode", true)) {
            setTheme(R.style.AppTheme);
        } else {
            setTheme(R.style.AppThemeLight);
        }
        setContentView(R.layout.activity_custom_speech);
        // Show the Up button in the action bar.
        setupActionBar();

        String BIGGER_THAN = ">";
        setTitle(getResources().getString(R.string.customSpeech).replace(BIGGER_THAN, ""));
        Button goButton = findViewById(R.id.btnGo);

        npFrom = findViewById(R.id.numberPickerFrom);
        npTo = findViewById(R.id.numberPickerTo);

        npFrom.setMinValue(1);
        npFrom.setMaxValue(99);
        npFrom.setWrapSelectorWheel(false);
        npFrom.setFocusableInTouchMode(true);
        npFrom.setFocusable(true);

        npTo.setMinValue(1);
        npTo.setMaxValue(99);
        npTo.setWrapSelectorWheel(false);
        npTo.setFocusableInTouchMode(true);
        npTo.setFocusable(true);

        goButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                int from = npFrom.getValue();
                int to = npTo.getValue();

                if (from >= to) {
                    showToast("Please specify a time range");
                } else {
                    // launch timer intent
                    Intent intent = new Intent(v.getContext(), TimerDisplayActivity.class);
                    intent.putExtra("key", getResources().getString(R.string.customSpeech) + ":" + from + ":" + to);
                    startActivity(intent);
                }
            }
        });
    }

    /**
     * Set up the {@link android.app.ActionBar}.
     */
    private void setupActionBar() {

        getActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.custom_speech, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_settings:
                //launch settings intent / activity
                Intent intent = new Intent(this, MyFragmentActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showToast(String msg)
    {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

}
