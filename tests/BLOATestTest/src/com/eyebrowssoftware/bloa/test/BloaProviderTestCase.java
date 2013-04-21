package com.eyebrowssoftware.bloa.test;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentResolver;

import com.eyebrowssoftware.bloa.Constants;
import com.eyebrowssoftware.bloa.data.BloaProvider;
import com.eyebrowssoftware.bloa.data.UserStatusRecords;
import com.eyebrowssoftware.bloa.data.UserStatusRecords.UserStatusRecord;
import com.eyebrowssoftware.bloa.data.UserTimelineRecords;
import com.eyebrowssoftware.bloa.data.UserTimelineRecords.UserTimelineRecord;

public class BloaProviderTestCase extends ProviderTestCase2<BloaProvider> {

    MockContentResolver mCR;

    public BloaProviderTestCase() {
        super(BloaProvider.class, BloaProvider.AUTHORITY);
    }

    public void setUp() throws Exception {
        super.setUp();
        mCR = this.getMockContentResolver();
        mCR.addProvider(BloaProvider.AUTHORITY, this.getProvider());
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testPreconditions() {
        assertNotNull(this.getProvider());
        assertNotNull(mCR);
        Cursor c = mCR.query(UserStatusRecords.CONTENT_URI, Constants.USER_STATUS_PROJECTION, null, null, null);
        assertNotNull(c);
        assertEquals(0, c.getCount());
        assertEquals(Constants.USER_STATUS_PROJECTION.length, c.getColumnCount());
        c.close();
        c = mCR.query(UserTimelineRecords.CONTENT_URI, Constants.USER_TIMELINE_PROJECTION, null, null, null);
        assertNotNull(c);
        assertEquals(0, c.getCount());
        assertEquals(Constants.USER_TIMELINE_PROJECTION.length, c.getColumnCount());
        c.close();
    }

    private Uri newUserStatusRecord(int id, long current) {
        ContentValues v = new ContentValues();
        v.put(UserStatusRecord.RECORD_ID, id);
        v.put(UserStatusRecord.USER_NAME, UserStatusRecord.USER_NAME + id);
        v.put(UserStatusRecord.USER_TEXT, UserStatusRecord.USER_TEXT + id);
        v.put(UserStatusRecord.USER_CREATED_DATE, String.valueOf(current));
        v.put(UserStatusRecord.CREATED_DATE, current);
        return mCR.insert(UserStatusRecords.CONTENT_URI, v);
    }

    private Cursor queryUserStatusRecord(Uri uri) {
        return mCR.query(uri, Constants.USER_STATUS_PROJECTION, null, null, UserStatusRecord.DEFAULT_SORT_ORDER);
    }

    public void testInsertUserStatus() {
        long current = System.currentTimeMillis();
        Uri uri = newUserStatusRecord(1, current);
        assertNotNull(uri);
        Cursor c = queryUserStatusRecord(uri);
        assertNotNull(c);
        assertEquals(1, c.getCount());
        assertEquals(Constants.USER_STATUS_PROJECTION.length, c.getColumnCount());
        assertTrue(c.moveToFirst());
        assertEquals(1, c.getInt(Constants.IDX_USER_STATUS_USER_ID));
        assertEquals(String.valueOf(current), c.getString(Constants.IDX_USER_STATUS_USER_CREATED_DATE));
        assertEquals(UserStatusRecord.USER_NAME + 1, c.getString(Constants.IDX_USER_STATUS_USER_NAME));
        assertEquals(UserStatusRecord.USER_TEXT + 1, c.getString(Constants.IDX_USER_STATUS_USER_TEXT));
        assertEquals(current, c.getLong(Constants.IDX_USER_STATUS_CREATED_DATE));
        c.close();
    }

    public void testUpdateUserStatus() {
        long current = System.currentTimeMillis();
        Uri uri = newUserStatusRecord(1, current);
        assertNotNull(uri);
        ContentValues v = new ContentValues();
        v.put(UserStatusRecord.USER_TEXT, UserStatusRecord.USER_TEXT + 2);
        assertEquals(1, mCR.update(uri, v, null, null));
        Cursor c = queryUserStatusRecord(uri);
        assertNotNull(c);
        assertEquals(1, c.getCount());
        assertEquals(Constants.USER_STATUS_PROJECTION.length, c.getColumnCount());
        assertTrue(c.moveToFirst());
        assertEquals(1, c.getInt(Constants.IDX_USER_STATUS_USER_ID));
        assertEquals(String.valueOf(current), c.getString(Constants.IDX_USER_STATUS_USER_CREATED_DATE));
        assertEquals(UserStatusRecord.USER_NAME + 1, c.getString(Constants.IDX_USER_STATUS_USER_NAME));
        assertEquals(UserStatusRecord.USER_TEXT + 2, c.getString(Constants.IDX_USER_STATUS_USER_TEXT));
        assertEquals(current, c.getLong(Constants.IDX_USER_STATUS_CREATED_DATE));
        c.close();
    }

    private int deleteUserStatusRecord(Uri uri) {
        return mCR.delete(uri, null, null);
    }

    public void testDeleteUserStatus() {
        long current = System.currentTimeMillis();
        Uri uri = newUserStatusRecord(1, current);
        assertEquals(1, deleteUserStatusRecord(uri));
        Cursor c = queryUserStatusRecord(uri);
        assertNotNull(c);
        assertEquals(0, c.getCount());
        c.close();
    }

    // User Timeline tests

    private Uri newUserTimelineRecord(int id, long current) {
        ContentValues v = new ContentValues();
        v.put(UserTimelineRecord.RECORD_ID, id);
        v.put(UserTimelineRecord.USER_NAME, UserTimelineRecord.USER_NAME + id);
        v.put(UserTimelineRecord.USER_TEXT, UserTimelineRecord.USER_TEXT + id);
        v.put(UserTimelineRecord.USER_CREATED_DATE, String.valueOf(current));
        v.put(UserTimelineRecord.CREATED_DATE, current);
        return mCR.insert(UserTimelineRecords.CONTENT_URI, v);
    }

    private Cursor queryUserTimelineRecord(Uri uri) {
        return mCR.query(uri, Constants.USER_TIMELINE_PROJECTION, null, null, UserTimelineRecord.DEFAULT_SORT_ORDER);
    }

    public void testInsertUserTimeline() {
        long current = System.currentTimeMillis();
        Uri uri = newUserTimelineRecord(1, current);
        assertNotNull(uri);
        Cursor c = queryUserTimelineRecord(uri);
        assertNotNull(c);
        assertEquals(1, c.getCount());
        assertEquals(Constants.USER_TIMELINE_PROJECTION.length, c.getColumnCount());
        assertTrue(c.moveToFirst());
        assertEquals(UserTimelineRecord.USER_NAME + 1, c.getString(Constants.IDX_USER_TIMELINE_USER_NAME));
        assertEquals(UserTimelineRecord.USER_TEXT + 1, c.getString(Constants.IDX_USER_TIMELINE_USER_TEXT));
        assertEquals(current, c.getLong(Constants.IDX_USER_TIMELINE_CREATED_DATE));
        c.close();
    }

    public void testUpdateUserTimeline() {
        long current = System.currentTimeMillis();
        Uri uri = newUserTimelineRecord(1, current);
        assertNotNull(uri);
        ContentValues v = new ContentValues();
        v.put(UserTimelineRecord.USER_TEXT, UserTimelineRecord.USER_TEXT + 2);
        assertEquals(1, mCR.update(uri, v, null, null));
        Cursor c = queryUserTimelineRecord(uri);
        assertNotNull(c);
        assertEquals(1, c.getCount());
        assertEquals(Constants.USER_TIMELINE_PROJECTION.length, c.getColumnCount());
        assertTrue(c.moveToFirst());
        assertEquals(UserTimelineRecord.USER_NAME + 1, c.getString(Constants.IDX_USER_TIMELINE_USER_NAME));
        assertEquals(UserTimelineRecord.USER_TEXT + 2, c.getString(Constants.IDX_USER_TIMELINE_USER_TEXT));
        assertEquals(current, c.getLong(Constants.IDX_USER_TIMELINE_CREATED_DATE));
        c.close();
    }

    private int deleteTimelineRecord(Uri uri) {
        return mCR.delete(uri, null, null);
    }

    public void testDeleteUserTimeline() {
        long current = System.currentTimeMillis();
        Uri uri = newUserTimelineRecord(1, current);
        assertEquals(1, deleteTimelineRecord(uri));
        Cursor c = queryUserTimelineRecord(uri);
        assertNotNull(c);
        assertEquals(0, c.getCount());
        c.close();
    }
}
