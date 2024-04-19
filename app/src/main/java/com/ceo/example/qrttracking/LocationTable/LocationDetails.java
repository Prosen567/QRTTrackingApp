package com.ceo.example.qrttracking.LocationTable;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.Nullable;
import android.util.Log;

import com.ceo.example.qrttracking.HelperSharedPreferences;
import com.ceo.example.qrttracking.OfflineDB;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

public class LocationDetails extends OfflineDB implements ILocationMast{

    public LocationDetails(@Nullable Context context) {
        super(context);
    }


    public boolean insertDataDistMast(String distNo, String acNo, String sectorNo, String secMobile, String IMEINo, String Lat, String Long, String updateDate, String flag, String Session, String IsOnline, String isSosUpdate) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        try {

            contentValues.put(DIST_NO,distNo);
            contentValues.put(AC_NO,acNo);
            contentValues.put(SECTOR_NO,sectorNo);
            contentValues.put(SEC_MOBILE_NO,secMobile);
            contentValues.put(IMEI_NO,IMEINo);
            contentValues.put(LAT,Lat);
            contentValues.put(LONG,Long);
            contentValues.put(UPDATE_DATE,updateDate);
            contentValues.put(FLAG,flag);
            contentValues.put(SESSION,Session);
            contentValues.put(ISONLINE,IsOnline);
            contentValues.put(SOSUPDATE,isSosUpdate);

            long result = db.insert(TABLE_LOCATION_MAST, null, contentValues);

            if (result == -1) {
                return false;

            } else
                return true;
        }
        catch (Exception e)
        {
            Log.d("Insertion Error : " , e.getMessage());
            return false;
        }
        finally {
            db.close();
        }
    }



    public boolean insertDataForEmergencyMast(String distNo, String acNo, String sectorNo, String secMobile, String IMEINo, String Lat, String Long, String updateDate, String flag, String Session, String IsOnline) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        try {

            contentValues.put(DIST_NO,distNo);
            contentValues.put(AC_NO,acNo);
            contentValues.put(SECTOR_NO,sectorNo);
            contentValues.put(SEC_MOBILE_NO,secMobile);
            contentValues.put(IMEI_NO,IMEINo);
            contentValues.put(LAT,Lat);
            contentValues.put(LONG,Long);
            contentValues.put(UPDATE_DATE,updateDate);
            contentValues.put(FLAG,flag);
            contentValues.put(SESSION,Session);
            contentValues.put(ISONLINE,IsOnline);

            long result = db.insert(TABLE_LOCATION_MAST_EMERGENCY, null, contentValues);

            if (result == -1) {
                return false;

            } else
                return true;
        }
        catch (Exception e)
        {
            Log.d("Insertion Error : " , e.getMessage());
            return false;
        }
        finally {
            db.close();
        }
    }


    public String getSpecificDataFlagInserted(String isonline, boolean isTrackingStopped) {
        List<Hashtable<String,String>> strinHashtableList=new ArrayList<>();
        String strSelectQueryColumns = COL_ID + ", " + DIST_NO + ", " + AC_NO + ", "+ SECTOR_NO + ", "+ SEC_MOBILE_NO +", "+IMEI_NO+", "+LAT+", "+LONG+", "+UPLOAD_DATE+", "+FLAG+", "+SESSION;
        String flag="";
        SQLiteDatabase db = this.getReadableDatabase();   //(last_name = 'Smith' AND first_name = 'Jane')
        Cursor res = db.rawQuery("select " + strSelectQueryColumns + " from " + TABLE_LOCATION_MAST + " Where " + ISONLINE + " = " + isonline,null);

        if(res.moveToFirst())
        {
            do {
                flag=res.getString(9);
                /*Hashtable<String,String> stringHashtable = new Hashtable<String, String>();
                stringHashtable.put("id", res.getString(0));
                stringHashtable.put("dist", res.getString(1));
                stringHashtable.put("ac", res.getString(2));
                stringHashtable.put("secno", res.getString(3));
                stringHashtable.put("secmobile", res.getString(4));
                stringHashtable.put("imei", res.getString(5));
                stringHashtable.put("lat", res.getString(6));
                stringHashtable.put("long", res.getString(7));
                stringHashtable.put("upload_date", res.getString(8));
                //stringHashtable.put("flag", res.getString(9));
                stringHashtable.put("session", res.getString(10));

                strinHashtableList.add(stringHashtable);*/
            }while (res.moveToNext());
        }
        int i = res.getCount();
        db.close();
        return flag;
    }

    public String[] getSpecificLatLong() {
        LinkedList<Hashtable<String,String>> strinHashtableList=new LinkedList<>();
        String strSelectQueryColumns = COL_ID + ", " + DIST_NO + ", " + AC_NO + ", "+ SECTOR_NO + ", "+ SEC_MOBILE_NO +", "+IMEI_NO+", "+LAT+", "+LONG+", "+UPLOAD_DATE+", "+FLAG+", "+SESSION;
        String Lat="",Long="";
        SQLiteDatabase db = this.getReadableDatabase();   //(last_name = 'Smith' AND first_name = 'Jane')
        Cursor res = db.rawQuery("select " + strSelectQueryColumns + " from " + TABLE_LOCATION_MAST,null);

        String[] latLong=new String[2];
        if(res.moveToLast())
        {
           // do {
                Lat=res.getString(6);
                Long=res.getString(7);
                latLong[0]=Lat;
                latLong[1]=Long;

/*
            }while (res.moveToPrevious());*/
        }
        int i = res.getCount();
        db.close();
        return latLong;

    }

    public Hashtable<String,String> getSpecificData(Context context,String isonline,int index) {
       // Hashtable<String,String> strinHashtableList=new Hashtable<>();
        String strSelectQueryColumns = COL_ID + ", " + DIST_NO + ", " + AC_NO + ", "+ SECTOR_NO + ", "+ SEC_MOBILE_NO +", "+IMEI_NO+", "+LAT+", "+LONG+", "+UPLOAD_DATE+", "+FLAG+", "+SESSION+", "+SOSUPDATE;
        Hashtable<String,String> stringHashtable=null;
        SQLiteDatabase db = this.getReadableDatabase();   //(last_name = 'Smith' AND first_name = 'Jane')
        Cursor res = db.rawQuery("select " + strSelectQueryColumns + " from " + TABLE_LOCATION_MAST + " Where " + ISONLINE + " = " + isonline/*+" and " + ISUPLOADING +" = "+isUploading*/,null);

        Cursor totalRow= db.rawQuery("select " + strSelectQueryColumns + " from " + TABLE_LOCATION_MAST,null);
        if (totalRow.getCount()>-1) {
            if (res.moveToNext()) {
                stringHashtable = new Hashtable<String, String>();
                stringHashtable.put("id", res.getString(0));
                stringHashtable.put("dist", res.getString(1));
                stringHashtable.put("ac", res.getString(2));
                stringHashtable.put("secno", res.getString(3));
                stringHashtable.put("secmobile", res.getString(4));
                stringHashtable.put("imei", res.getString(5));
                stringHashtable.put("lat", res.getString(6));
                stringHashtable.put("long", res.getString(7));
                stringHashtable.put("upload_date", res.getString(8));
                stringHashtable.put("flag", res.getString(9));
                stringHashtable.put("session", res.getString(10));
                stringHashtable.put("sos_update", res.getString(11));
                stringHashtable.put("tablesize", String.valueOf(res.getCount()));

                // strinHashtableList.add(stringHashtable);
            }
        }
        HelperSharedPreferences.putSharedPreferencesString(context,"tablesize", String.valueOf(totalRow.getCount()));
        int i = res.getCount();
        db.close();
        return stringHashtable;
    }

    public List<Hashtable<String,String>> getSpecificDataForEmergency(String isonline) {
        List<Hashtable<String,String>> strinHashtableList=new ArrayList<>();
        String strSelectQueryColumns = COL_ID + ", " + DIST_NO + ", " + AC_NO + ", "+ SECTOR_NO + ", "+ SEC_MOBILE_NO +", "+IMEI_NO+", "+LAT+", "+LONG+", "+UPLOAD_DATE+", "+FLAG+", "+SESSION;

        SQLiteDatabase db = this.getReadableDatabase();   //(last_name = 'Smith' AND first_name = 'Jane')
        Cursor res = db.rawQuery("select " + strSelectQueryColumns + " from " + TABLE_LOCATION_MAST_EMERGENCY + " Where " + ISONLINE + " = " + isonline,null);

        if(res.moveToFirst())
        {
            do {
                Hashtable<String,String> stringHashtable = new Hashtable<String, String>();
                stringHashtable.put("id", res.getString(0));
                stringHashtable.put("dist", res.getString(1));
                stringHashtable.put("ac", res.getString(2));
                stringHashtable.put("secno", res.getString(3));
                stringHashtable.put("secmobile", res.getString(4));
                stringHashtable.put("imei", res.getString(5));
                stringHashtable.put("lat", res.getString(6));
                stringHashtable.put("long", res.getString(7));
                stringHashtable.put("upload_date", res.getString(8));
                stringHashtable.put("flag", res.getString(9));
                stringHashtable.put("session", res.getString(10));

                strinHashtableList.add(stringHashtable);
            }while (res.moveToNext());
        }

        int i = res.getCount();
        db.close();
        return strinHashtableList;
    }
    public boolean updateData(String id, String isonline12) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        try {
            contentValues.put(ISONLINE,isonline12);
            long result = db.update(TABLE_LOCATION_MAST,contentValues,COL_ID + " =  "+id,null);
            if (result == -1) {
                return false;
            } else
                return true;
        }
        catch (Exception e)
        {
            Log.d("Insertion Error : " , e.getMessage());
            return false;
        }
        finally {
            db.close();
        }
    }

    public void deleteData(String id){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LOCATION_MAST, COL_ID+" = " + id, null);
        db.close();

    }
    public boolean updateSOSData(String id, String isUploading) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        try {
            contentValues.put(SOSUPDATE,isUploading);
            long result = db.update(TABLE_LOCATION_MAST,contentValues,COL_ID + " =  "+id,null);
            if (result == -1) {
                return false;
            } else
                return true;
        }
        catch (Exception e)
        {
            Log.d("Insertion Error : " , e.getMessage());
            return false;
        }
        finally {
            db.close();
        }
    }
    public boolean updateDataForEmergency(String id, String isonline) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        try {
            contentValues.put(ISONLINE,isonline);
            long result = db.update(TABLE_LOCATION_MAST_EMERGENCY,contentValues,COL_ID + " = ? ",new String[]{id});
            if (result == -1) {
                return false;
            } else
                return true;
        }
        catch (Exception e)
        {
            Log.d("Insertion Error: " , e.getMessage());
            return false;
        }
        finally {
            db.close();
        }
    }
    public List<Hashtable<String,String>> getAllData() {
        List<Hashtable<String,String>> strinHashtableList=new ArrayList<>();
        String selectQuery = "Select * from "+TABLE_LOCATION_MAST;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        if(cursor.moveToFirst())
        {
            do {
                Hashtable<String,String> stringHashtable = new Hashtable<String, String>();
                stringHashtable.put("id", cursor.getString(0));
                stringHashtable.put("dist", cursor.getString(1));
                stringHashtable.put("ac", cursor.getString(2));
                stringHashtable.put("secno", cursor.getString(3));
                stringHashtable.put("secmobile", cursor.getString(4));
                stringHashtable.put("imei", cursor.getString(5));
                stringHashtable.put("lat", cursor.getString(6));
                stringHashtable.put("long", cursor.getString(7));
                stringHashtable.put("upload_date", cursor.getString(8));
                stringHashtable.put("flag", cursor.getString(9));
                stringHashtable.put("session", cursor.getString(10));

                strinHashtableList.add(stringHashtable);
            }while (cursor.moveToNext());
        }

        int i = cursor.getCount();
        db.close();
        return strinHashtableList;
        /*Cursor cursor = getAllData();  //cursor hold all your data
        JSONObject jobj ;
        JSONArray arr = new JSONArray();
        cursor.moveToFIrst();
        while(cursor.moveToNext()) {
            jobj = new JSONObject();
            jboj.put("Id", cursor.getInt("Id"));
            jboj.put("Name", cursor.getString("Name"));
            arr.put(jobj);
        }

        jobj = new JSONObject();
        jobj.put("data", arr);

        String st = jboj.toString();*/
    }
}
