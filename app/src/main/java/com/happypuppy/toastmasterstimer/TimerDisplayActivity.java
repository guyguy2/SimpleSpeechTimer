package com.happypuppy.toastmasterstimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import androidx.core.app.NavUtils;
import androidx.core.content.ContextCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.happypuppy.toastmasterstimer.persistence.Dto;
import com.happypuppy.toastmasterstimer.persistence.PersistenceHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TimerDisplayActivity extends Activity {
    private String greenTime;
    private String yellowTime;
    private String redTime;
    private Button startButton;
    private Button resetButton;
    private Button saveButton;
    private RelativeLayout currentLayout = null;
    private static final int REMOVE_BACKGROUND_COLOR = -1;
    private boolean isTimerRunning = false;
    private Chronometer mChronometer;
    private long timeWhenStopped = 0;
    private boolean useVibrate;
    private boolean useSound;
    private boolean useOrangeBackgroundColor;
    private Ringtone ringtone;
    private boolean useMaterialDesign = false;
    private boolean isShown = true;
    private PersistenceHelper dbHelper = null;
    private Map<Integer, Integer> statusBarColorMap = new HashMap<>();
    private final Animation stoppedAnimation = new AlphaAnimation(1.0f, 0.2f);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_dark_mode", true)) {
            setTheme(R.style.AppTheme);
        } else {
            setTheme(R.style.AppThemeLight);
        }

        setContentView(R.layout.activity_timer_display);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Show the Up button in the action bar.
        setupActionBar();

        //handleParams
        setupParams();

        //setup timer - extract
        mChronometer = findViewById(R.id.chronometer1);
        startButton = findViewById(R.id.btnStart);
        startButton.setTextColor(Color.GREEN);
        this.currentLayout = findViewById(R.id.timerLayout);

        useVibrate = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_vibrate", false);
        useSound = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_sound", false);
        useOrangeBackgroundColor = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_color", false);

        mChronometer.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                String displayedTime = String.valueOf(chronometer.getText());

                if (displayedTime.equals(greenTime)) {
                    changeBackgroundColor(Color.GREEN);
                } else if (displayedTime.equals(yellowTime)) {
                    if (useOrangeBackgroundColor) {
                        changeBackgroundColor(Color.rgb(255, 165, 0));
                    }
                    else {
                        changeBackgroundColor(Color.YELLOW);
                    }
                }
                else if (displayedTime.equals(redTime)) {
                    changeBackgroundColor(Color.RED);
                }
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
                    startTimer();
                    mChronometer.clearAnimation();
                }
            }
        });

        resetButton = findViewById(R.id.btnReset);
        resetButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                stopTimer();
                mChronometer.clearAnimation();
                changeBackgroundColor(REMOVE_BACKGROUND_COLOR); //TODO refactor to take color from XML
                mChronometer.setBase(SystemClock.elapsedRealtime());
                timeWhenStopped = 0;
                enableSaveButton(false);
            }
        });
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        this.useMaterialDesign = true;

        saveButton = findViewById(R.id.btnSave);
        enableSaveButton(false);
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
        this.statusBarColorMap.put(Color.rgb(255, 165, 0), Color.rgb(250,140,0)); //orange

        //Animation
        this.stoppedAnimation.setDuration(700);
        this.stoppedAnimation.setRepeatMode(Animation.REVERSE);
        this.stoppedAnimation.setRepeatCount(Animation.INFINITE);

        if (savedInstanceState != null) { //restore state (orientation change)
            mChronometer.setText(savedInstanceState.getCharSequence("time"));
            isTimerRunning = savedInstanceState.getBoolean("isRunning");
            long currentTime = savedInstanceState.getLong("currentTime");
            timeWhenStopped = savedInstanceState.getLong("timeWhenStopped");
            isShown = !savedInstanceState.getBoolean("isShown");
            toggleShowHideElements();
            if (!isTimerRunning) {
                enableSaveButton(savedInstanceState.getBoolean("saveButtonState"));
            } else {
                startButton.setText(R.string.stop_btn_txt);
                startButton.setTextColor(Color.RED);
                toggleStartButtonIcon();
                isTimerRunning = true;
                mChronometer.setBase(SystemClock.elapsedRealtime() + currentTime);
                mChronometer.start();
                enableSaveButton(false);
                mChronometer.clearAnimation();
            }
            if (savedInstanceState.getInt("color") != 0) {
                changeBackgroundColor(savedInstanceState.getInt("color"));
            }
        }
    }

    private void enableSaveButton(boolean isEnabled) {
        Drawable saveIcon = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_save_white_24dp);
        if (isEnabled) {
            saveButton.setEnabled(true);
            saveIcon.setAlpha(255);
        }
        else {
            saveButton.setEnabled(false);
            saveIcon.setAlpha(100);
        }
        saveButton.setCompoundDrawablesWithIntrinsicBounds(saveIcon, null, null, null);
    }

    private void persistTime() {
        //popup dialog to get name
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.save_time));
        builder.setMessage(getString(R.string.enter_speaker_name));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        input.requestFocus();
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY | InputMethodManager.HIDE_NOT_ALWAYS);
        builder.setView(input);
        builder.setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (input.getText().toString().isEmpty()) {
                    Toast.makeText(getApplicationContext(), getString(R.string.nothing_to_save), Toast.LENGTH_SHORT).show();
                    View view = findViewById(android.R.id.content);
                    if (view != null) {
                        imm.toggleSoftInput(0, 0);
                    }
                    input.clearFocus();
                    return;
                }
                input.clearFocus();
                imm.toggleSoftInput(0, 0);
                Dto.name = input.getText().toString();
                Dto.speechTime = mChronometer.getText().toString();
                Dto.type = getActionBar().getTitle().toString().substring(0, getActionBar().getTitle().toString().indexOf('('));
                DateFormat dateFormatISO8601 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Dto.timestamp = dateFormatISO8601.format(new Date());

                writeToDb();
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                input.clearFocus();
                imm.toggleSoftInput(0, 0);
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void writeToDb() {
        Log.d("TimerDisplayActivity", "writeToDb " + this.dbHelper.getDatabaseName());
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
                    " (" + Arrays.toString(PersistenceHelper.COLUMNS).replace("[", "").replace("]", "")
                    + ") VALUES("
                    + ++Dto.id + ", '"
                    + Dto.name + "','"
                    + Dto.type + "', '"
                    + Dto.speechTime + "', '"
                    + Dto.timestamp + "');");
            Toast.makeText(getApplicationContext(),  getString(R.string.saving_time) + Dto.name + ", " + Dto.speechTime, Toast.LENGTH_SHORT).show();
        } catch (SQLiteConstraintException e) {
            Log.e("ERROR", e.getLocalizedMessage());
            Toast.makeText(getApplicationContext(),  getString(R.string.error_db), Toast.LENGTH_SHORT).show();
        } finally {
            db.close();
        }
    }

    private void startTimer() {
        startButton.setText(R.string.stop_btn_txt);
        if (isBackgroundColor(Color.RED)) {
            startButton.setTextColor(Color.BLACK);
        } else {
            startButton.setTextColor(Color.RED);
        }
        toggleStartButtonIcon();
        isTimerRunning = true;
        mChronometer.setBase(SystemClock.elapsedRealtime() + timeWhenStopped);
        mChronometer.start();
        enableSaveButton(false);
    }

    private void stopTimer() {
        startButton.setText(R.string.start_btn);
        if (isBackgroundColor(Color.GREEN)) {
            startButton.setTextColor(Color.BLACK);
        } else {
            startButton.setTextColor(Color.GREEN);
        }
        toggleStartButtonIcon();
        isTimerRunning = false;
        timeWhenStopped = mChronometer.getBase() - SystemClock.elapsedRealtime();
        mChronometer.stop();
        enableSaveButton(true);
    }

    private boolean isBackgroundColor(int color) {
        if (currentLayout == null || currentLayout.getBackground() == null) {
            return false;
        }
        int backgroundColor = ((ColorDrawable)currentLayout.getBackground()).getColor();

        return backgroundColor == color;
    }

    private void toggleStartButtonIcon() {
        if (startButton.getText().equals(getString(R.string.start_btn))){
            Drawable playIcon = ContextCompat.getDrawable(this, android.R.drawable.ic_media_play);
            startButton.setCompoundDrawablesWithIntrinsicBounds(playIcon, null, null, null);
        }
        else {
            Drawable pauseIcon = ContextCompat.getDrawable(this, android.R.drawable.ic_media_pause);
            startButton.setCompoundDrawablesWithIntrinsicBounds(pauseIcon, null, null, null);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.e("","*****");
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
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
            this.greenTime = String.format(Locale.getDefault(),"%02d:00", Integer.parseInt(times[1]));
            this.redTime = String.format(Locale.getDefault(),"%02d:00", Integer.parseInt(times[2]));

            setTitle("Speech (" + times[1] + " to " + times[2]+ " min)");
        }
        else if (type.equals(res.getString(R.string.speechEval)))
        {
            setTitle(res.getString(R.string.speechEval).replace(BIGGER_THAN, ""));
            this.greenTime = "02:00";
            this.redTime = "03:00";
        }
        else if (type.equals(res.getString(R.string.tableTopicsZeroToOne)))
        {
            setTitle(res.getString(R.string.tableTopicsZeroToOne).replace(BIGGER_THAN, ""));
            this.greenTime = "00:30";
            this.redTime = "01:00";
            this.yellowTime = "00:45";
            return;
        }

        this.yellowTime = calculateYellowTime();
    }

    private String calculateYellowTime() {
        int yellow;
        boolean isHalf = false;

        int temp = (Integer.parseInt(greenTime.split(":")[0]) + Integer.parseInt(redTime.split(":")[0]));
        if (temp % 2 != 0) {
            isHalf = true;
        }
        yellow = temp / 2;

        if (isHalf) {
            return String.format(Locale.getDefault(), "%02d:30", yellow);
        } else {
            return String.format(Locale.getDefault(),"%02d:00", yellow);
        }
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
                Intent intent = new Intent(this, MyFragmentActivity.class);
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
            icon = ContextCompat.getDrawable(this, R.drawable.ic_visibility_white_24dp);
        }
        else {
            icon = ContextCompat.getDrawable(this, R.drawable.ic_visibility_off_white_24dp);
        }
        menuItem.setIcon(icon);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void invalidateOptionsMenu() {
        super.invalidateOptionsMenu();
    }

    private void toggleShowHideElements() {
        final float ALPHA_INVISIBLE = 0f;
        final float ALPHA_VISIBLE = 1f;
        invalidateOptionsMenu();
        if (isShown) {
            this.startButton.setVisibility(View.GONE);
            this.resetButton.setVisibility(View.GONE);
            this.mChronometer.clearAnimation();
            this.mChronometer.setAlpha(ALPHA_INVISIBLE);
            this.saveButton.setVisibility(View.GONE);
            this.isShown = false;
        }
        else {
            this.startButton.setVisibility(View.VISIBLE);
            this.resetButton.setVisibility(View.VISIBLE);
            this.mChronometer.setAlpha(ALPHA_VISIBLE);
            if (!isTimerRunning) {
                this.mChronometer.setAnimation(this.stoppedAnimation);
            }
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
                triggerCues(color);
            }
        }
    }

    private void triggerCues(int color) {
        if (this.useSound) {
            try {
                this.ringtone.play();
            } catch (Exception e) {
                Log.e("TimerDisplayActivity", "Error in triggerCues");
                e.printStackTrace();
            }
        }
        if (this.useVibrate) {
            //can add patterns in the future
            if (color == Color.RED) {
                long pattern[]={0,300,300,300};
                ((Vibrator)getSystemService(Context.VIBRATOR_SERVICE)).vibrate(pattern, -1);
            }
            else {
                ((Vibrator)getSystemService(Context.VIBRATOR_SERVICE)).vibrate(300);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequence("time", mChronometer.getText());
        outState.putLong("timeWhenStopped", timeWhenStopped);
        outState.putLong("currentTime", mChronometer.getBase() - SystemClock.elapsedRealtime());
        if (currentLayout.getBackground() != null) {
            outState.putInt("color", ((ColorDrawable)currentLayout.getBackground()).getColor());
        }
        outState.putBoolean("isRunning", isTimerRunning);
        outState.putBoolean("saveButtonState", saveButton.isEnabled());
        outState.putBoolean("isShown", isShown);
    }
}
