package com.ceo.example.qrttracking.LocationTable;

public interface ILocationMast {
    //Column Names
    String COL_ID = "ID";
    String DIST_NO = "DIST_NO";
    String AC_NO = "AC_NO";
    String SECTOR_NO = "SECTOR_NO";
    String SEC_MOBILE_NO = "SEC_MOBILE_NO";
    String IMEI_NO = "IMEI_NO";
    String LAT = "LAT";
    String LONG = "LONG";
    String UPLOAD_DATE = "UPLOAD_DATE";
    String UPDATE_DATE = "UPDATE_DATE";
    String FLAG = "FLAG";
    String SESSION = "SESSION";
    String ISONLINE = "ISONLINE";
    String SOSUPDATE = "ISSOSUPDATE";

    //Table Name
    String TABLE_LOCATION_MAST = "QRT_TRACKING";
    String TABLE_LOCATION_MAST_EMERGENCY = "QRT_TRACKING_EMERGENCY";

    //Create Table Query
    String CREATE_TABLE_LOCATION_MAST = "create table " + TABLE_LOCATION_MAST + " (" +
            COL_ID + " INTEGER PRIMARY KEY, " +
            DIST_NO + " VARCHAR, " +
            AC_NO + " INTEGER, " +
            SECTOR_NO + " INTEGER, " +
            SEC_MOBILE_NO + " VARCHAR, " +
            IMEI_NO + " VARCHAR, " +
            LAT + " VARCHAR, " +
            LONG + " VARCHAR, " +
            UPDATE_DATE + " VARCHAR, " +
            FLAG + " VARCHAR, " +
            SESSION + " VARCHAR, " +
            ISONLINE + " INTEGER, " +
            SOSUPDATE + " INTEGER, " +
            UPLOAD_DATE + " DATETIME DEFAULT (DATETIME('now','localtime'))" +
            ")";

    String CREATE_TABLE_LOCATION_MAST_EMERGENCY = "create table " + TABLE_LOCATION_MAST_EMERGENCY + " (" +
            COL_ID + " INTEGER PRIMARY KEY, " +
            DIST_NO + " VARCHAR, " +
            AC_NO + " INTEGER, " +
            SECTOR_NO + " INTEGER, " +
            SEC_MOBILE_NO + " VARCHAR, " +
            IMEI_NO + " VARCHAR, " +
            LAT + " VARCHAR, " +
            LONG + " VARCHAR, " +
            UPDATE_DATE + " VARCHAR, " +
            FLAG + " VARCHAR, " +
            SESSION + " VARCHAR, " +
            ISONLINE + " INTEGER, " +
            UPLOAD_DATE + " DATETIME DEFAULT (DATETIME('now','localtime'))" +
            ")";

    //Drop Table Query
    String DROP_TABLE_LOCATION_MAST = "DROP TABLE IF EXISTS " + TABLE_LOCATION_MAST;
    String DROP_TABLE_LOCATION_MAST_EMERGENCY = "DROP TABLE IF EXISTS " + TABLE_LOCATION_MAST_EMERGENCY;
}
