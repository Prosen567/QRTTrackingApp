package com.ceo.example.qrttracking;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;
import android.util.Log;

import com.ceo.example.qrttracking.LocationTable.ILocationMast;


public class OfflineDB extends SQLiteOpenHelper {
    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "EVMTracker.db";
    ILocationMast locationMast;
    public OfflineDB(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(locationMast.CREATE_TABLE_LOCATION_MAST);
        db.execSQL(locationMast.CREATE_TABLE_LOCATION_MAST_EMERGENCY);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion > oldVersion) {
            Log.w("MyAppTag", "Updating database from version " + oldVersion + " to "
                    + newVersion + " .Existing data will be lost.");
            db.execSQL(locationMast.DROP_TABLE_LOCATION_MAST );
            db.execSQL(locationMast.DROP_TABLE_LOCATION_MAST_EMERGENCY );
            onCreate(db);
        }
    }

}
