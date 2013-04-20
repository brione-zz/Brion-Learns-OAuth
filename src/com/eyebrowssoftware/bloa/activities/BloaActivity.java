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
package com.eyebrowssoftware.bloa.activities;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;

import junit.framework.Assert;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.eyebrowssoftware.bloa.BloaApp;
import com.eyebrowssoftware.bloa.Constants;
import com.eyebrowssoftware.bloa.IKeysProvider;
import com.eyebrowssoftware.bloa.MyKeysProvider;
import com.eyebrowssoftware.bloa.R;
import com.eyebrowssoftware.bloa.data.UserStatusRecords;
import com.eyebrowssoftware.bloa.data.UserStatusRecords.UserStatusRecord;

public class BloaActivity extends FragmentActivity implements LoaderCallbacks<Cursor>,
        AccountManagerCallback<Bundle> {
    public static final String TAG = "BloaActivity";

    private CheckBox mCB;
    private EditText mEditor;
    private Button mButton;
    private TextView mUserTextView;
    private TextView mLastTweetTextView;

    private OAuthConsumer mConsumer = null;
    private AccountManager mAm;
    private Account mAccount;
    private ContentResolver mCR;

    private Handler mHandler = new Handler();

    private Boolean mIsLoggedIn;

    // You'll need to create this or change the name of DefaultKeysProvider
    IKeysProvider mKeysProvider = new MyKeysProvider();

    // TODO: decide to move the minSdkVersion forward or not
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        mCR = this.getContentResolver();
        mConsumer = BloaApp.getOAuthConsumer();

        mCB = (CheckBox) this.findViewById(R.id.enable);
        mCB.setOnClickListener(new LoginCheckBoxClickedListener());

        mEditor = (EditText) this.findViewById(R.id.editor);

        mButton = (Button) this.findViewById(R.id.post);
        mButton.setOnClickListener(new PostButtonClickListener());

        mUserTextView = (TextView) this.findViewById(R.id.user);
        mLastTweetTextView = (TextView) this.findViewById(R.id.last);

        mAm = AccountManager.get(this);
        Account[] accounts = mAm.getAccountsByType(Constants.ACCOUNT_TYPE);
        Assert.assertTrue(accounts.length < 2); // There can only be one: Twitter
        if (accounts.length > 0) {
            mAccount = accounts[0];
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAccount != null) {
            // Assume we're logged in, for now, so we can move some code here
            mCB.setChecked(mIsLoggedIn);
            mButton.setEnabled(mIsLoggedIn);
            mEditor.setEnabled(mIsLoggedIn);
            if (mIsLoggedIn) {
                TimelineSelector ss = new TimelineSelector(Constants.HOME_TIMELINE_URL_STRING);
                new GetTimelineTask().execute(ss);
            } else {
                deleteStatusRecord();
                deleteTimelineRecords();
            }
         }
    }
    private int deleteTimelineRecords() {
        return mCR.delete(UserStatusRecords.CONTENT_URI, Constants.USER_TIMELINE_QUERY_WHERE, null);
    }

    private void deleteStatusRecord() {
        mCR.delete(UserStatusRecords.CONTENT_URI, Constants.USER_STATUS_QUERY_WHERE, null);
    }

    private ContentValues parseTimelineJSONObject(JSONObject object) throws JSONException {
        ContentValues values = new ContentValues();
        JSONObject user = object.getJSONObject("user");
        values.put(UserStatusRecord.USER_NAME, user.getString("name"));
        values.put(UserStatusRecord.RECORD_ID, user.getInt("id_str"));
        values.put(UserStatusRecord.USER_CREATED_DATE, object.getString("created_at"));
        values.put(UserStatusRecord.USER_TEXT, object.getString("text"));
        return values;
    }

    class PostButtonClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            String postString = mEditor.getText().toString();
            if (postString.length() == 0) {
                Toast.makeText(BloaActivity.this, getText(R.string.tweet_empty), Toast.LENGTH_SHORT).show();
            } else {
                new PostTask().execute(postString);
            }
        }
    }

    private void setLoggedOut() {
        // XXX: Fix thix
        // mAm.invalidateAuthToken(Constants.ACCOUNT_TYPE, null);
        deleteStatusRecord();
        deleteTimelineRecords();
        mButton.setEnabled(false);
        mEditor.setEnabled(false);
        mEditor.setText(null);
        updateUI(null, null);

    }

    class LoginCheckBoxClickedListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            if(mCB.isChecked()) {
                if (mAccount != null) {
                    mAm.getAuthToken(mAccount, Constants.AUTHTOKEN_TYPE, true, BloaActivity.this, null);
                } else {
                    mAm.addAccount(Constants.ACCOUNT_TYPE, Constants.AUTHTOKEN_TYPE, null, null, BloaActivity.this, BloaActivity.this, mHandler);
                }
            } else {
                setLoggedOut();
            }
            mCB.setChecked(false); // the oauth callback will set it to the proper state
        }
    }

    private ContentValues parseVerifyUserJSONObject(JSONObject object) throws JSONException {
        ContentValues values = new ContentValues();
        values.put(UserStatusRecord.USER_NAME, object.getString("name"));
        values.put(UserStatusRecord.RECORD_ID, object.getInt("id_str"));
        values.put(UserStatusRecord.USER_CREATED_DATE, object.getString("created_at"));
        JSONObject status = object.getJSONObject("status");
        values.put(UserStatusRecord.USER_TEXT, status.getString("text"));
        return values;
    }


    class GetCredentialsTask extends AsyncTask<Void, Void, Boolean> {

        HttpClient mClient = BloaApp.getHttpClient();
        @Override
        protected Boolean doInBackground(Void... arg0) {
            JSONObject jso = null;
            HttpGet get = new HttpGet(Constants.VERIFY_URL_STRING);
            try {
                mConsumer.sign(get);
                String response = mClient.execute(get, new BasicResponseHandler());
                if (response != null) {
                    jso = new JSONObject(response);
                    BloaApp.makeNewUserStatusRecord(BloaActivity.this.getContentResolver(), parseVerifyUserJSONObject(jso));
                    return true;
                } else {
                    Log.e(TAG, "PostTask: null response text");
                    throw new IllegalStateException("Expected some text in the Http response");
                }
            } catch (Exception e) {
                // Expected if we don't have the proper credentials saved away
            }
            return false;
        }

        // This is in the UI thread, so we can mess with the UI
        @Override
        protected void onPostExecute(Boolean loggedIn) {
            super.onPostExecute(loggedIn);
       }
    }



    //----------------------------
    // This task posts a message to your message queue on the service.
    class PostTask extends AsyncTask<String, Void, Void> {

        ProgressDialogFragment mDialog;
        HttpClient mClient = BloaApp.getHttpClient();

        @Override
        protected void onPreExecute() {
            mDialog = ProgressDialogFragment.newInstance(R.string.tweet_progress_title, R.string.tweet_progress_text);
            mDialog.show(getSupportFragmentManager(), "auth");
        }

        @Override
        protected Void doInBackground(String... params) {

            JSONObject jso = null;
            try {

                HttpPost post = new HttpPost(Constants.STATUSES_URL_STRING);
                LinkedList<BasicNameValuePair> out = new LinkedList<BasicNameValuePair>();
                out.add(new BasicNameValuePair("status", params[0]));
                post.setEntity(new UrlEncodedFormEntity(out, HTTP.UTF_8));
                // sign the request to authenticate
                mConsumer.sign(post);
                String response = mClient.execute(post, new BasicResponseHandler());
                if (response != null) {
                    jso = new JSONObject(response);
                    BloaApp.makeNewUserStatusRecord(mCR, parseTimelineJSONObject(jso));
                } else {
                    Log.e(TAG, "PostTask: null response text");
                    throw new IllegalStateException("Expected some text in the Http response");
                }
            } catch (HttpResponseException e) {
                int status = e.getStatusCode();
                if (status == HttpStatus.SC_UNAUTHORIZED) {
                    // TODO: The user secret is invalid, so we've got to invalidate it in the account
                    Log.e(TAG, "Unauthorized status returned: User secret needs to be invalidated in the account", e);
                } else {
                    e.printStackTrace();
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (OAuthMessageSignerException e) {
                e.printStackTrace();
            } catch (OAuthExpectationFailedException e) {
                e.printStackTrace();
            } catch (OAuthCommunicationException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        // This is in the UI thread, so we can mess with the UI
        protected void onPostExecute(Void nada) {
            super.onPostExecute(nada);
            mDialog.dismiss();
            BloaActivity.this.mEditor.setText(null);
        }
    }


    class TimelineSelector extends Object {
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

    class GetTimelineTask extends AsyncTask<TimelineSelector, Void, Void> {

        ProgressDialogFragment mDialog;
        HttpClient mClient = BloaApp.getHttpClient();

        @Override
        protected void onPreExecute() {
            mDialog = ProgressDialogFragment.newInstance(R.string.timeline_progress_title, R.string.timeline_progress_text);
            mDialog.show(BloaActivity.this.getSupportFragmentManager(), "auth");
        }

        @Override
        protected Void doInBackground(TimelineSelector... params) {
            JSONArray array = null;
            try {
                Uri sUri = Uri.parse(params[0].url);
                Uri.Builder builder = sUri.buildUpon();
                if(params[0].since_id != null) {
                    builder.appendQueryParameter("since_id", String.valueOf(params[0].since_id));
                } else if (params[0].max_id != null) { // these are mutually exclusive
                    builder.appendQueryParameter("max_id", String.valueOf(params[0].max_id));
                }
                if(params[0].count != null) {
                    builder.appendQueryParameter("count", String.valueOf((params[0].count > 200) ? 200 : params[0].count));
                }
                if(params[0].page != null) {
                    builder.appendQueryParameter("page", String.valueOf(params[0].page));
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
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, status.toString());
                        }
                        values[i] = parseTimelineJSONObject(status);
                    }
                    mCR.bulkInsert(UserStatusRecords.CONTENT_URI, values);
                } else {
                    Log.e(TAG, "GetTimelineTask: null response text");
                    throw new IllegalStateException("Expected some text in the Http response");
                }
            } catch (Exception e) {
                Log.e(TAG, "Get Timeline Exception", e);
            }
            return null;
        }

        // This is in the UI thread, so we can mess with the UI
        @Override
        protected void onPostExecute(Void nada) {
            mDialog.dismiss();
        }
}

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle savedValues) {
        // Create a CursorLoader that will take care of creating a cursor for the data
        return new CursorLoader(this, UserStatusRecords.CONTENT_URI,
            Constants.USER_STATUS_PROJECTION, Constants.USER_STATUS_QUERY_WHERE,
            null, UserStatusRecord.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // We got something but it might be empty
        String name = null, last = null;
        if (cursor.moveToFirst()) {
            name = cursor.getString(Constants.IDX_USER_STATUS_USER_NAME);
            last = cursor.getString(Constants.IDX_USER_STATUS_USER_TEXT);
        }
        updateUI(name, last);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        updateUI(null, null);
    }

    private void updateUI(String userName, String lastMessage) {
        if (userName != null && lastMessage != null) {
            mUserTextView.setText(userName);
            mLastTweetTextView.setText(lastMessage);
        } else {
            mUserTextView.setText(getString(R.string.userhint));
            mLastTweetTextView.setText(getString(R.string.userhint));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.bloa_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.refresh_timeline:
            deleteTimelineRecords();
            TimelineSelector ss = new TimelineSelector(Constants.HOME_TIMELINE_URL_STRING);
            new GetTimelineTask().execute(ss);
            return true;
        default:
            return false;
        }
    }

    @Override
    public void run(AccountManagerFuture<Bundle> futureResult) {
        Log.d(TAG, "Got a future!");
        String token = null;
        String secret = null;
        try {
            Bundle authResult = futureResult.getResult();
            token = authResult.getString(AccountManager.KEY_ACCOUNT_NAME);
            secret = authResult.getString(AccountManager.KEY_AUTHTOKEN);
            mConsumer.setTokenWithSecret(token, secret);
            new GetCredentialsTask().execute();
        } catch (OperationCanceledException e) {
            e.printStackTrace();
        } catch (AuthenticatorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
