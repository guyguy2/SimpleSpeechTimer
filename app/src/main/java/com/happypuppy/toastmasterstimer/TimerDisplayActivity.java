package com.happypuppy.toastmasterstimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.happypuppy.toastmasterstimer.persistence.Dto;
import com.happypuppy.toastmasterstimer.persistence.PersistenceHelper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TimerDisplayActivity extends Activity {
    private static String START = "Start";
    private static String STOP = "Stop";
    private String greenTime;
    private String yellowTime;
    private String redTime;
    private Button startButton;
    private Button resetButton;
    private Button saveButton;
    private RelativeLayout currentLayout = null;
    private static int REMOVE_BACKGROUND_COLOR = -1;
    private boolean isTimerRunning = false;
    private Chronometer mChronometer;
    private CircleTimerView mCircle; ///
    private long timeWhenStopped = 0;
    private boolean useVibrate;
    private boolean useSound;
    private Ringtone ringtone;
    private boolean useMaterialDesign = false;
    private boolean isShown = true;
    private PersistenceHelper dbHelper = null;
    private Map<Integer, Integer> statusBarColorMap = new HashMap<>();
    private Animation stoppedAnimation = new AlphaAnimation(1.0f, 0.1f);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer_display);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Show the Up button in the action bar.
        setupActionBar();

        //handleParams
        setupParams();

        //setup timer - extract
        mChronometer = (Chronometer) findViewById(R.id.chronometer1);
        mCircle = (CircleTimerView)findViewById(R.id.stopwatch_time); ///
        mCircle.startIntervalAnimation();///
        startButton = (Button) findViewById(R.id.btnStart);
        startButton.setTextColor(Color.GREEN);
        this.currentLayout = (RelativeLayout)findViewById(R.id.timerLayout);

        useVibrate = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_vibrate", false);
        useSound = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_sound", false);

        mChronometer.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String time = s.toString();

                if (time.equals(greenTime)) {
                    changeBackgroundColor(Color.GREEN);
                } else if (time.equals(yellowTime))
                    changeBackgroundColor(Color.YELLOW);
                else if (time.equals(redTime))
                    changeBackgroundColor(Color.RED);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isTimerRunning) {
                    //pause
                    stopTimer();
                    mChronometer.startAnimation(stoppedAnimation);
                } else {
                    startButton.setText(STOP);
                    startButton.setTextColor(Color.RED);
                    toggleStartButtonIcon();
                    isTimerRunning = true;
                    mChronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
                    mChronometer.start();
                    saveButton.setEnabled(false);
                    mChronometer.clearAnimation();
                }
            }
        });

        resetButton = (Button) findViewById(R.id.btnReset);
        resetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                stopTimer();
                mChronometer.clearAnimation();
                changeBackgroundColor(REMOVE_BACKGROUND_COLOR); //TODO refactor to take color from XML
                mChronometer.setBase(SystemClock.elapsedRealtime());
                timeWhenStopped = 0;
                saveButton.setEnabled(false);
            }
        });
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.useMaterialDesign = true;
        }
        saveButton = (Button)findViewById(R.id.btnSave);
        saveButton.setEnabled(false);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                persistTime();
            }
        });
        this.dbHelper = new PersistenceHelper(this);
        //darker shade for status bar color
        this.statusBarColorMap.put(Color.GREEN, Color.rgb(0, 200, 0));
        this.statusBarColorMap.put(Color.YELLOW, Color.rgb(200, 200, 0));
        this.statusBarColorMap.put(Color.RED, Color.rgb(200, 0, 0));

        //Animation
        this.stoppedAnimation.setDuration(700);
        this.stoppedAnimation.setRepeatMode(Animation.REVERSE);
        this.stoppedAnimation.setRepeatCount(Animation.INFINITE);
    }

    private void persistTime() {
        //popup dialog to get name
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save Time");
        builder.setMessage("Enter speaker's name");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (input.getText().toString().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Nothing to save", Toast.LENGTH_SHORT).show();
                    return;
                }
//                Dto dto = new Dto();
                Dto.name = input.getText().toString();
                Dto.speechTime = mChronometer.getText().toString();
                Dto.type = getActionBar().getTitle().toString().substring(0, getActionBar().getTitle().toString().indexOf('('));
                Time t = new Time(Time.getCurrentTimezone());
                t.setToNow();
                Dto.timestamp = t.format("%m/%d/%Y %H:%M");
                writeToDb();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void writeToDb() {
        Log.i("TimerDisplayActivity", "writeToDb " + this.dbHelper.getDatabaseName()); ///
//        this.deleteDatabase(dbHelper.getDatabaseName()); ///
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        //get max ID
        Cursor c = db.rawQuery("SELECT _id from "
                + dbHelper.getDatabaseName() + " order by _id DESC LIMIT 1"
                , null);

        if (c.moveToNext()) {
            Dto.id = c.getInt(0);
        }

        try {
            db.execSQL("INSERT INTO " + dbHelper.getDatabaseName() +
                    " (" + Arrays.toString(dbHelper.COLUMNS).replace("[", "").replace("]", "")
                    + ") VALUES("
                    + ++Dto.id + ", '"
                    + Dto.name + "','"
                    + Dto.type + "', '"
                    + Dto.speechTime + "', '"
                    + Dto.timestamp + "');");
            Toast.makeText(getApplicationContext(),  "Saving " + Dto.name + "'s time of " + Dto.speechTime, Toast.LENGTH_SHORT).show();
        } catch (SQLiteConstraintException e) {
            Log.e("ERROR", e.getLocalizedMessage());
            Toast.makeText(getApplicationContext(),  "Error writing to database. Please delete the data and try again", Toast.LENGTH_SHORT).show();
        } finally {
            db.close();
        }
    }

    private void stopTimer() {
        startButton.setText(START);
        startButton.setTextColor(Color.GREEN);
        toggleStartButtonIcon();
        isTimerRunning = false;
        timeWhenStopped = mChronometer.getBase() - SystemClock.elapsedRealtime();
        mChronometer.stop();
        saveButton.setEnabled(true);
    }

    private void toggleStartButtonIcon() {
        if (startButton.getText().equals(START)){
            Drawable playIcon = getResources().getDrawable( android.R.drawable.ic_media_play );
            startButton.setCompoundDrawablesWithIntrinsicBounds(playIcon, null, null, null);
        }
        else {
            Drawable pauseIcon = getResources().getDrawable( android.R.drawable.ic_media_pause );
            startButton.setCompoundDrawablesWithIntrinsicBounds(pauseIcon, null, null, null);
        }
    }


    private void setupParams() {
        Intent intent = getIntent();
        String type = intent.getStringExtra("key");

        Resources res = getResources();

        String BIGGER_THAN = ">";
        if (type.equals(res.getString(R.string.tableTopics)))
        {
            setTitle(res.getString(R.string.tableTopics).replace(BIGGER_THAN, ""));
            this.greenTime = "01:00";
            this.redTime = "02:00";
        }
        else if (type.equals(res.getString(R.string.iceBreaker)))
        {
            setTitle(res.getString(R.string.iceBreaker).replace(BIGGER_THAN, ""));
            this.greenTime = "04:00";
            this.redTime = "06:00";
        }
        else if (type.equals(res.getString(R.string.speech)))
        {
            setTitle(res.getString(R.string.speech).replace(BIGGER_THAN, ""));
            this.greenTime = "05:00";
            this.redTime = "07:00";
        }
        else if (type.startsWith(res.getString(R.string.customSpeech)))
        {
            String[] times = type.split(":");
            this.greenTime = String.format("%02d:00", Integer.parseInt(times[1]));
            this.redTime = String.format("%02d:00", Integer.parseInt(times[2]));

            setTitle("Speech (" + times[1] + " to " + times[2]+ " min)");
        }
        else if (type.equals(res.getString(R.string.speechEval)))
        {
            setTitle(res.getString(R.string.speechEval).replace(BIGGER_THAN, ""));
            this.greenTime = "02:00";
            this.redTime = "03:00";
        }
        else if (type.equals(res.getString(R.string.eightToTen)))
        {
            setTitle(res.getString(R.string.eightToTen).replace(BIGGER_THAN, ""));
            this.greenTime = "08:00";
            this.redTime = "10:00";
        }
        int yellow;
        boolean isHalf = false;
        int temp = (Integer.parseInt(greenTime.split(":")[0]) + Integer.parseInt(redTime.split(":")[0]));
        if (temp % 2 != 0)
            isHalf = true;

        yellow = temp / 2;

        if (isHalf)
            this.yellowTime = String.format("%02d:30", yellow);
        else
            this.yellowTime = String.format("%02d:00", yellow);
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
        getMenuInflater().inflate(R.menu.timer_display, menu);
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
                Intent intent = new Intent(this, PreferencesActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_show_hide:
                toggleShowHideElements();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.action_show_hide);
        Drawable icon;
        if (isShown) {
             icon = getResources().getDrawable(R.drawable.ic_visibility_white_24dp );
        }
        else {
             icon = getResources().getDrawable( R.drawable.ic_visibility_off_white_24dp);
        }
        menuItem.setIcon(icon);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void invalidateOptionsMenu() {
        super.invalidateOptionsMenu();
    }

    private void toggleShowHideElements() {
        invalidateOptionsMenu();
        if (isShown) {
            this.startButton.setVisibility(View.GONE);
            this.resetButton.setVisibility(View.GONE);
            this.mChronometer.setVisibility(View.GONE);
            this.saveButton.setVisibility(View.GONE);
            this.isShown = false;
        }
        else {
            this.startButton.setVisibility(View.VISIBLE);
            this.resetButton.setVisibility(View.VISIBLE);
            this.mChronometer.setVisibility(View.VISIBLE);
            this.saveButton.setVisibility(View.VISIBLE);
            this.isShown = true;
        }
    }

    private void changeBackgroundColor(int color) {
        if (currentLayout != null)
        {
            if (color == REMOVE_BACKGROUND_COLOR)
            {
                currentLayout.setBackgroundResource(0);
                mChronometer.setTextColor(Color.WHITE);
                resetButton.setTextColor(Color.WHITE);
                startButton.setTextColor(Color.GREEN);

                if (useMaterialDesign) {
                    this.getWindow().setStatusBarColor(Color.BLACK);
                    getActionBar().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
                    getWindow().setNavigationBarColor(Color.BLACK);
                }
            }
            else
            {
                currentLayout.setBackgroundColor(color);
                mChronometer.setTextColor(Color.BLACK);
                resetButton.setTextColor(Color.BLACK);
                startButton.setTextColor(Color.RED);

                if (useMaterialDesign) {
                    this.getWindow().setStatusBarColor(statusBarColorMap.get(color));
                    this.getActionBar().setBackgroundDrawable(new ColorDrawable(color));
                    getWindow().setNavigationBarColor(color);
                }
                triggerCues();
            }
        }
    }

    private void triggerCues() {
        if (this.useSound) {
            try {
                this.ringtone.play();
            } catch (Exception e) {
                Log.e("TimerDisplayActivity", "Error in triggerCues");
                e.printStackTrace();
            }
        }
        if (this.useVibrate) {
            ((Vibrator)getSystemService(Context.VIBRATOR_SERVICE)).vibrate(250);
        }
    }

}
