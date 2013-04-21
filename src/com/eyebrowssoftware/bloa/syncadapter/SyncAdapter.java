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
package com.eyebrowssoftware.bloa.syncadapter;

import java.io.IOException;

import junit.framework.Assert;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.eyebrowssoftware.bloa.BloaApp;
import com.eyebrowssoftware.bloa.Constants;
import com.eyebrowssoftware.bloa.IKeysProvider;
import com.eyebrowssoftware.bloa.data.UserStatusRecords;
import com.eyebrowssoftware.bloa.data.UserStatusRecords.UserStatusRecord;
import com.eyebrowssoftware.bloa.data.UserTimelineRecords;
import com.eyebrowssoftware.bloa.data.UserTimelineRecords.UserTimelineRecord;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider.  This sample shows a basic 2-way
 * sync between the client and a sample server.  It also contains an
 * example of how to update the contacts' status messages, which
 * would be useful for a messaging or social networking client.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    static final String TAG = "SyncAdapter";

    private HttpClient mClient = BloaApp.getHttpClient();
    private final TimelineSelector mTimelineSelector = new TimelineSelector(Constants.HOME_TIMELINE_URL_STRING);
    private OAuthConsumer mConsumer;
    private static final boolean NOTIFY_AUTH_FAILURE = true;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        Log.d(TAG, "Sync adapter constructor");
        IKeysProvider keys = BloaApp.getKeysProvider();
        mConsumer = new CommonsHttpOAuthConsumer(keys.getKey1(), keys.getKey2());
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        // Use the account manager to request the AuthToken we'll need
        // to talk to our sample server.  If we don't have an AuthToken
        // yet, this could involve a round-trip to the server to request
        // and AuthToken.
            AccountManager am = AccountManager.get(this.getContext());
            String authtoken;
            try {
                authtoken = am.blockingGetAuthToken(account, Constants.AUTHTOKEN_TYPE, NOTIFY_AUTH_FAILURE);
                String token = am.getUserData(account, Constants.PARAM_USERNAME);
                mConsumer.setTokenWithSecret(token, authtoken);
                syncUserProfile(provider, syncResult);
                syncUserTimeline(provider, syncResult);
            } catch (OperationCanceledException e) {
                e.printStackTrace();
            } catch (AuthenticatorException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    private void syncUserProfile(ContentProviderClient provider, SyncResult syncResult) {
        Log.i(TAG, "Sync adapter: onPerformSync");

        JSONObject jso = null;
        // User Status Sync
        HttpGet get = new HttpGet(Constants.VERIFY_URL_STRING);
        try {
            mConsumer.sign(get);
            String response = mClient.execute(get, new BasicResponseHandler());
            if (response != null) {

                // Singleton: Delete any existing records for user
                provider.delete(UserStatusRecords.CONTENT_URI, null, null);

                // Distinguish this as a User Status singleton, regardless of origin
                jso = new JSONObject(response);
                ContentValues values = new ContentValues();
                values.put(UserStatusRecord.USER_NAME, jso.getString("name"));
                values.put(UserStatusRecord.RECORD_ID, jso.getInt("id_str"));
                values.put(UserStatusRecord.USER_CREATED_DATE, jso.getString("created_at"));
                JSONObject status = jso.getJSONObject("status");
                values.put(UserStatusRecord.USER_TEXT, status.getString("text"));

                Assert.assertNotNull(provider.insert(UserStatusRecords.CONTENT_URI, values));
            } else {
                Log.e(TAG, "Sync User Status: null response text");
                throw new IllegalStateException("Expected some text in the Http response");
            }
        } catch (final IOException e) {
            e.printStackTrace();
            syncResult.stats.numIoExceptions++;
        } catch (final ParseException e) {
            e.printStackTrace();
            syncResult.stats.numParseExceptions++;
        } catch (final JSONException e) {
            e.printStackTrace();
            syncResult.stats.numParseExceptions++;
        } catch (RemoteException e) {
            syncResult.stats.numIoExceptions++;
            e.printStackTrace();
        } catch (OAuthMessageSignerException e) {
            e.printStackTrace();
            syncResult.stats.numAuthExceptions++;
        } catch (OAuthExpectationFailedException e) {
            syncResult.stats.numAuthExceptions++;
            e.printStackTrace();
        } catch (OAuthCommunicationException e) {
            e.printStackTrace();
            syncResult.stats.numIoExceptions++;
        }
    }

    private void syncUserTimeline(ContentProviderClient provider, SyncResult syncResult) {
        JSONArray array = null;
        try {
            provider.delete(UserTimelineRecords.CONTENT_URI, null, null);
            Uri sUri = Uri.parse(mTimelineSelector.url);
            Uri.Builder builder = sUri.buildUpon();
            if(mTimelineSelector.since_id != null) {
                builder.appendQueryParameter("since_id", String.valueOf(mTimelineSelector.since_id));
            } else if (mTimelineSelector.max_id != null) { // these are mutually exclusive
                builder.appendQueryParameter("max_id", String.valueOf(mTimelineSelector.max_id));
            }
            if(mTimelineSelector.count != null) {
                builder.appendQueryParameter("count", String.valueOf((mTimelineSelector.count > 200) ? 200 : mTimelineSelector.count));
            }
            if(mTimelineSelector.page != null) {
                builder.appendQueryParameter("page", String.valueOf(mTimelineSelector.page));
            }
            HttpGet get = new HttpGet(builder.build().toString());
            mConsumer.sign(get);
            String response = mClient.execute(get, new BasicResponseHandler());
            if (response != null) {
                array = new JSONArray(response);
                // Delete the existing timeline
                ContentValues[] values = new ContentValues[array.length()];
                for(int i = 0; i < array.length(); ++i) {
                    JSONObject status = array.getJSONObject(i);
                    values[i] = new ContentValues();
                    JSONObject user = status.getJSONObject("user");
                    values[i].put(UserTimelineRecord.USER_NAME, user.getString("name"));
                    values[i].put(UserTimelineRecord.RECORD_ID, user.getInt("id_str"));
                    values[i].put(UserTimelineRecord.USER_CREATED_DATE, status.getString("created_at"));
                    values[i].put(UserTimelineRecord.USER_TEXT, status.getString("text"));
                }
                Assert.assertEquals(array.length(), provider.bulkInsert(UserTimelineRecords.CONTENT_URI, values));
                Log.d(TAG, String.format("Inserted %1$d items into database", array.length()));
            } else {
                Log.e(TAG, "GetTimelineTask: null response text");
                throw new IllegalStateException("Expected some text in the Http response");
            }
        } catch (final IOException e) {
            e.printStackTrace();
            syncResult.stats.numIoExceptions++;
        } catch (final ParseException e) {
            e.printStackTrace();
            syncResult.stats.numParseExceptions++;
        } catch (final JSONException e) {
            e.printStackTrace();
            syncResult.stats.numParseExceptions++;
        } catch (RemoteException e) {
            syncResult.stats.numIoExceptions++;
            e.printStackTrace();
        } catch (OAuthMessageSignerException e) {
            e.printStackTrace();
            syncResult.stats.numAuthExceptions++;
        } catch (OAuthExpectationFailedException e) {
            syncResult.stats.numAuthExceptions++;
            e.printStackTrace();
        } catch (OAuthCommunicationException e) {
            e.printStackTrace();
            syncResult.stats.numIoExceptions++;
        }

    }

    /**
     * TODO:
     *
     * This will be used to select the type of timeline the user wants to see
     * via the sync preferences
     * @author brionemde
     *
     */
    class TimelineSelector {
        public String url; // the url to perform the query from
        // not all these apply to every url - you are responsible
        public Long since_id; // ids newer than this will be fetched
        public Long max_id; // ids older than this will be fetched
        public Integer count; // # of tweets to fetch Max is 200
        public Integer page; // # of page to fetch (with limits)

        public TimelineSelector(String u) {
            url = u;
            max_id = null;
            since_id = null;
            count = null;
            page = null;
        }

        public TimelineSelector(String u, Long since, Long max, Integer cnt, Integer pg) {
            url = u;
            max_id = max;
            since_id = since;
            count = cnt;
            page = pg;
        }
    }


}

