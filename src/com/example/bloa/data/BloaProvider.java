/*
 * Copyright 2013 - Brion Noble Emde
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.example.bloa.data;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.example.bloa.data.UserStatusRecords.UserStatusRecord;


/**
 * Provides access to a database of notes. Each note has a title, the note
 * itself, a creation date and a modified data.
 */
public class BloaProvider extends ContentProvider {
    static final String TAG = BloaProvider.class.toString();

    public static final String AUTHORITY = "com.example.bloa";

    public static final String URI_STRING = "content://" + AUTHORITY;

    private static final String USER_STATUS_RECORDS_TABLE_NAME = "user_status_records";

    public static final String USER_STATUS_PATH = "user_status";
    public static final String USER_TIMELINE_PATH = "user_timeline";

    private static final String DATABASE_NAME = "bloa.db";

    private static final int DB_VERSION_1 = 1;

    private static final int DATABASE_VERSION = DB_VERSION_1;

    private static HashMap<String, String> sUserStatusProjectionMap;

    private static final UriMatcher sUriMatcher;

    private static final int USER_STATUS_RECORDS = 1;
    private static final int USER_STATUS_RECORD_ID = 2;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, USER_STATUS_PATH, USER_STATUS_RECORDS);
        sUriMatcher.addURI(AUTHORITY, USER_STATUS_PATH + "/#", USER_STATUS_RECORD_ID);

        sUserStatusProjectionMap = new HashMap<String, String>();
        sUserStatusProjectionMap.put(UserStatusRecord._ID, UserStatusRecord._ID);
        sUserStatusProjectionMap.put(UserStatusRecord.RECORD_ID, UserStatusRecord.RECORD_ID);
        sUserStatusProjectionMap.put(UserStatusRecord.USER_NAME, UserStatusRecord.USER_NAME);
        sUserStatusProjectionMap.put(UserStatusRecord.USER_TEXT, UserStatusRecord.USER_TEXT);
        sUserStatusProjectionMap.put(UserStatusRecord.CREATED_DATE, UserStatusRecord.CREATED_DATE);
        sUserStatusProjectionMap.put(UserStatusRecord.USER_CREATED_DATE, UserStatusRecord.USER_CREATED_DATE);
        sUserStatusProjectionMap.put(UserStatusRecord.LATEST_STATUS, UserStatusRecord.LATEST_STATUS);
}
    /**
     *
     */
    public static final Uri CONTENT_URI = Uri.parse(URI_STRING);

    private ContentResolver mCR;

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            db.execSQL("CREATE TABLE " + USER_STATUS_RECORDS_TABLE_NAME + " ("
                    + UserStatusRecord._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + UserStatusRecord.RECORD_ID + " INTEGER,"
                    + UserStatusRecord.USER_NAME + " TEXT,"
                    + UserStatusRecord.USER_TEXT + " TEXT,"
                    + UserStatusRecord.CREATED_DATE + " INTEGER,"
                    + UserStatusRecord.USER_CREATED_DATE + " TEXT,"
                    + UserStatusRecord.LATEST_STATUS + " TEXT DEFAULT NULL"
                    + ");");
}

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion == DB_VERSION_1) {
                // Nothing to do yet
            }
        }
    }

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        Context c = getContext();
        mOpenHelper = new DatabaseHelper(c);
        mCR = c.getContentResolver();
        return true;
    }

    @Override
    public void finalize() {
        mOpenHelper.close();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        Cursor c = null;

        switch (sUriMatcher.match(uri)) {
        case USER_STATUS_RECORDS:
            qb.setTables(USER_STATUS_RECORDS_TABLE_NAME);
            qb.setProjectionMap(sUserStatusProjectionMap);
            c = qb.query(db, projection, selection, selectionArgs, null, null,
                    sortOrder);
            break;
        case USER_STATUS_RECORD_ID:
            qb.setTables(USER_STATUS_RECORDS_TABLE_NAME);
            qb.setProjectionMap(sUserStatusProjectionMap);
            qb.appendWhere(UserStatusRecords.UserStatusRecord._ID + "="
                    + uri.getPathSegments().get(1));
            c = qb.query(db, projection, selection, selectionArgs, null, null,
                    sortOrder);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        // Tell the cursor what uri to watch, so it knows when its source data
        // changes
        c.setNotificationUri(mCR, uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case USER_STATUS_RECORDS:
            return UserStatusRecords.CONTENT_TYPE;
        case USER_STATUS_RECORD_ID:
            return UserStatusRecord.CONTENT_ITEM_TYPE;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) throws SQLException {

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long item_id = -1;
        ContentValues values = new ContentValues();
        if (initialValues != null) {
            values = initialValues;
        } else {
            values = new ContentValues();
        }
        switch (sUriMatcher.match(uri)) {
        case USER_STATUS_RECORDS:
            item_id = db.insert(USER_STATUS_RECORDS_TABLE_NAME,
                    UserStatusRecords.UserStatusRecord.CREATED_DATE, values);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        Uri ret = null;
        if (item_id > 0) {
            ret = ContentUris.withAppendedId(UserStatusRecords.CONTENT_URI,
                    item_id);
            mCR.notifyChange(ret, null);
        }
        return ret;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        int count = 0;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String recId;
        String whereClause;

        switch (sUriMatcher.match(uri)) {
        case USER_STATUS_RECORDS:
            count = db.delete(USER_STATUS_RECORDS_TABLE_NAME, where, whereArgs);
            break;
        case USER_STATUS_RECORD_ID:
            recId = uri.getPathSegments().get(1);
            whereClause = UserStatusRecord._ID + "=" + recId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");
            count = db.delete(USER_STATUS_RECORDS_TABLE_NAME, whereClause, whereArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        mCR.notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where,
            String[] whereArgs) {
        int count = 0;
        // Get the database and run the query
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String recId;
        String whereClause;

        switch (sUriMatcher.match(uri)) {
        case USER_STATUS_RECORDS:
            count = db.update(USER_STATUS_RECORDS_TABLE_NAME, values, where, whereArgs);
            break;
        case USER_STATUS_RECORD_ID:
            recId = uri.getPathSegments().get(1);
            whereClause = UserStatusRecord._ID + "=" + recId
                    + (!TextUtils.isEmpty(where) ? " AND (" + where + ")" : "");
            count = db.update(USER_STATUS_RECORDS_TABLE_NAME, values, whereClause, whereArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        mCR.notifyChange(uri, null);
        return count;
    }
}
