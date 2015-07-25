package com.happypuppy.toastmasterstimer.persistence;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite DB to persist speakers' speech times
 * Created by Guy on 5/24/2015.
 */
public class PersistenceHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "simpleTimer";
    private static final int DATABASE_VERSION = 1;
    public static final String[] COLUMNS = {"_id", "NAME", "TYPE", "TIME", "TIMESTAMP"};

    public PersistenceHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + DATABASE_NAME);
        db.execSQL("CREATE TABLE " + DATABASE_NAME + " ("
                + "_id" + " INTEGER PRIMARY KEY,"
                + "NAME" + " TEXT,"
                + "TYPE" + " TEXT,"
                + "TIME" + " TEXT,"
                + "TIMESTAMP TEXT"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new UnsupportedOperationException("onUpgrade");
    }
}
