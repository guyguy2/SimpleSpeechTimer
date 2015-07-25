package com.happypuppy.toastmasterstimer;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.happypuppy.toastmasterstimer.persistence.PersistenceHelper;

public class MainMenuActivity extends Activity {

    private static final String EXTRA_MESSAGE = "key";
    private PersistenceHelper dbHelper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        this.dbHelper = new PersistenceHelper(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    public void launchCustomDisplay(View view)
    {
        Intent intent = new Intent(this, com.happypuppy.toastmasterstimer.CustomSpeechActivity.class);
        startActivity(intent);
    }

    public void launchTimerDisplay(View view)
    {
        Intent intent = new Intent(this, TimerDisplayActivity.class);
        String speechType = null;

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
            case R.id.eightToTenBtn:
                speechType = getResources().getString(R.string.eightToTen);
                break;
            default:
                break;
        }

        if (speechType != null)
        {
            intent.putExtra(EXTRA_MESSAGE, speechType);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
            }
            else {
                startActivity(intent);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_about:
                showToast("Simple Speech Timer version " + this.getString(R.string.version));
                return true;
            case R.id.action_settings:
                Intent intent = new Intent(this, PreferencesActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_feedback:
                sendEmail();
                return true;
            case R.id.action_clear_db:
                clearDb();
                return true;
            case R.id.action_read_db:
                readFromDb();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void sendEmail(){

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:" + this.getString(R.string.contact_developer_uri)));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Simple Speech Timer Feedback");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Hi,");

        try {
            startActivity(Intent.createChooser(emailIntent, "Send email using..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No email clients installed.", Toast.LENGTH_SHORT).show();
        }

    }

    private void clearDb() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm");
        builder.setMessage("Delete all saved data?");
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.delete(dbHelper.getDatabaseName(), null, null);
                showToast("Saved data deleted");
            }
        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
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
        ///TODO CursorAdapter
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        if (!db.isOpen()) {
            Log.e("DB","db closed");///
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
        builder.setTitle("Saved Data");

        if (c.getCount() == 0) {
            showToast("No Data");
            return;
        }
        MyCursorAdapter cursorAdapter = new MyCursorAdapter(this, c);
        builder.setAdapter(cursorAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setNeutralButton("Share", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                StringBuilder sb = new StringBuilder();
                sb.append("Name | Speech Type | Time | Timestamp\n\n");
                c.moveToPosition(-1);
                while (c.moveToNext()) {

                    sb.append(c.getString(c.getColumnIndex("NAME")));
                    sb.append(" ");
                    sb.append(c.getString(c.getColumnIndex("TYPE")));
                    sb.append(" ");
                    sb.append(c.getString(c.getColumnIndex("TIME")));
                    sb.append(" ");
                    sb.append(c.getString(c.getColumnIndex("TIMESTAMP")));
                    sb.append('\n');
                }
                String shareBody = sb.toString();
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Toastmasters Timer Report");
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                startActivity(Intent.createChooser(sharingIntent, "Share via"));
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
     }

    private void showToast(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private static class MyCursorAdapter extends CursorAdapter {
        public MyCursorAdapter(Context context, Cursor cursor) {
            super(context, cursor, 0);
        }

        // The newView method is used to inflate a new view and return it,
        // you don't bind any data to the view at this point.
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.show_data_view, parent, false);
        }

        // The bindView method is used to bind all data to a given view
        // such as setting the text on a TextView.
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Find fields to populate in inflated template
            TextView tvName = (TextView) view.findViewById(R.id.tvName);
            TextView tvTime = (TextView) view.findViewById(R.id.tvTime);
            TextView tvType = (TextView) view.findViewById(R.id.tvType);
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
