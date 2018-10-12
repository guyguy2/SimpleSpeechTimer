package com.happypuppy.toastmasterstimer;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.happypuppy.toastmasterstimer.persistence.PersistenceHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainMenuActivity extends Activity {

    private static final String EXTRA_MESSAGE = "key";
    private static final String SPACE = " ";
    private PersistenceHelper dbHelper = null;
    private Button buttonClicked = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_dark_mode", true)) {
            setTheme(R.style.AppTheme);
        } else {
            setTheme(R.style.AppThemeLight);
        }
        setContentView(R.layout.activity_main_menu);
        this.dbHelper = new PersistenceHelper(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    public void launchCustomDisplay(View view) {
        Intent intent = new Intent(this, com.happypuppy.toastmasterstimer.CustomSpeechActivity.class);
        startActivity(intent);
    }

    public void launchTimerDisplay(View view) {
        Intent intent = new Intent(this, TimerDisplayActivity.class);
        String speechType = null;
        buttonClicked = findViewById(view.getId());
        buttonClicked.setOnLongClickListener(new View.OnLongClickListener() { //should work?
            @Override
            public boolean onLongClick(View v) {
                showToast("Long from " + v);
                return true;
            }
        });

        switch (view.getId()) {

            case R.id.tableTopicsBtn:
                speechType = getResources().getString(R.string.tableTopics);
                break;
            case R.id.iceBreakerBtn:
                speechType = getResources().getString(R.string.iceBreaker);
                break;
            case R.id.speechBtn:
                speechType = getResources().getString(R.string.speech);
                break;
            case R.id.customSpeechBtn:
                speechType = getResources().getString(R.string.customSpeech);
                break;
            case R.id.speechEvalBtn:
                speechType = getResources().getString(R.string.speechEval);
                break;
            case R.id.tableTopicsZeroToOneBtn:
                speechType = getResources().getString(R.string.tableTopicsZeroToOne);
                break;
            default:
                break;
        }

        if (speechType != null) {
            intent.putExtra(EXTRA_MESSAGE, speechType);
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, MyFragmentActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_feedback:
                sendFeedbackEmail();
                return true;
            case R.id.action_read_db:
                readFromDb();
                return true;
            case R.id.action_rate_app:
                rateApp();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void rateApp() {
        PackageManager pm = getPackageManager();
        String installerPackageName = pm.getInstallerPackageName(getPackageName());
        boolean fromGooglePlay = false;
        if (installerPackageName != null) {
            if (installerPackageName.contains("android.vending")) {
                fromGooglePlay = true;
            } else if (installerPackageName.contains("amazon")) {
                fromGooglePlay = false;
            }
        } else {
            Toast.makeText(this, "No App Store found", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse((fromGooglePlay ? "market://details?id=" : "amzn://apps/android?p=") + getPackageName())));
        } catch (ActivityNotFoundException e1) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse((fromGooglePlay ? "http://play.google.com/store/apps/details?id=" : "http://www.amazon.com/gp/mas/dl/android?p=") + getPackageName())));
            } catch (ActivityNotFoundException e2) {
                Toast.makeText(this, "You don't have any app that can open this link", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void sendFeedbackEmail() {
        String versionName = BuildConfig.VERSION_NAME;

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:" + this.getString(R.string.contact_developer_uri)));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Simple Speech Timer Feedback");
        StringBuilder sb = new StringBuilder();
        sb.append("Device: " + Build.MANUFACTURER + " " + Build.MODEL + ", Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + "), ");
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);
        sb.append("resolution: " + size.x + "x" + size.y + "\n");
        sb.append("Version: " + versionName);
        sb.append("\n- - - - -\n\n");

        emailIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());

        try {
            startActivity(Intent.createChooser(emailIntent, getString(R.string.send_email_using)));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No email clients installed.", Toast.LENGTH_SHORT).show();
        }

    }

    private String getTodaysDate() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy", java.util.Locale.getDefault());
        return sdf.format(c.getTime());
    }

    private void clearDb() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.confirm));
        builder.setMessage(getString(R.string.delete_all_saved_data));
        builder.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.delete(dbHelper.getDatabaseName(), null, null);
                showToast(getString(R.string.saved_data_deleted));
            }
        });

        builder.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    private void readFromDb() {
        // read from DB and show on screen
        ///TODO Use Room lib
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (!db.isOpen()) {
            Log.e("DB", "db closed");
        }
        final Cursor c = db.query(dbHelper.getDatabaseName(),
                PersistenceHelper.COLUMNS,
                null, //selection
                null, //selectionArgs
                null, //groupBy
                null, //having
                null); //orderBy

//        Log.e("DB", "row count after query: " + c.getCount()); ///
//        Log.e("DB", "" + c.getCount() + "; " + DatabaseUtils.dumpCursorToString(c)); ///

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.saved_data));

        if (c.getCount() == 0) {
            showToast(getString(R.string.no_data));
            return;
        }
        MyCursorAdapter cursorAdapter = new MyCursorAdapter(this, c);
        builder.setAdapter(cursorAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setNeutralButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                clearDb();
            }
        });

        builder.setPositiveButton(getString(R.string.share), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                StringBuilder sb = new StringBuilder();
                sb.append(getString(R.string.email_header) + "\n\n");
                c.moveToPosition(-1);
                int i = 0;
                while (c.moveToNext()) {
                    sb.append(++i + ". ");
                    sb.append(SPACE);
                    sb.append(c.getString(c.getColumnIndex("NAME")));
                    sb.append(SPACE);
                    sb.append(c.getString(c.getColumnIndex("TYPE")));
                    sb.append(SPACE);
                    sb.append(c.getString(c.getColumnIndex("TIME")));
                    sb.append(SPACE);
//                    sb.append(c.getString(c.getColumnIndex("TIMESTAMP")));
                    sb.append('\n');
                }
                sb.append("\n\n");
                sb.append("Created using Simple Speech Timer for Android\n");
                String shareBody = sb.toString();
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.email_subject_tm_timer_report) + " - " + getTodaysDate());
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_via)));
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
        alert.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.RED);
        alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(Color.GREEN);
    }

    private void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private static class MyCursorAdapter extends CursorAdapter {
        MyCursorAdapter(Context context, Cursor cursor) {
            super(context, cursor, 0);
        }

        // The newView method is used to inflate a new view and return it,
        // you don't bind any data to the view at this point.
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.show_data_view, parent, false);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //get reference to the row
            View view = super.getView(position, convertView, parent);
            //check for odd or even to set alternate colors to the row background
            if (position % 2 == 0) {
                view.setBackgroundColor(Color.rgb(238, 233, 233));
            } else {
                view.setBackgroundColor(Color.rgb(255, 255, 255));
            }

            return view;
        }

        // The bindView method is used to bind all data to a given view
        // such as setting the text on a TextView.
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Find fields to populate in inflated template
            TextView tvName = view.findViewById(R.id.tvName);
            TextView tvTime = view.findViewById(R.id.tvTime);
            TextView tvType = view.findViewById(R.id.tvType);
//            TextView tvTimestamp = (TextView) view.findViewById(R.id.tvTimestamp);
            // Extract properties from cursor
            String name = cursor.getString(cursor.getColumnIndexOrThrow("NAME"));
            String time = cursor.getString(cursor.getColumnIndexOrThrow("TIME"));
            String type = cursor.getString(cursor.getColumnIndexOrThrow("TYPE"));
//            String timestamp = cursor.getString(cursor.getColumnIndexOrThrow("TIMESTAMP"));
            // Populate fields with extracted properties
            tvName.setText(name);
            tvTime.setText(time);
            tvType.setText(type);
//            tvTimestamp.setText(timestamp);
        }
    }
}
