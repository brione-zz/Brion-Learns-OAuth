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

import junit.framework.Assert;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
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
import com.eyebrowssoftware.bloa.data.BloaProvider;
import com.eyebrowssoftware.bloa.data.UserStatusRecords;
import com.eyebrowssoftware.bloa.data.UserTimelineRecords;
import com.eyebrowssoftware.bloa.data.UserStatusRecords.UserStatusRecord;

public class BloaActivity extends FragmentActivity implements LoaderCallbacks<Cursor>, AccountManagerCallback<Bundle> {
    public static final String TAG = "BloaActivity";

    private CheckBox mCB;
    private EditText mEditor;
    private Button mButton;
    private TextView mUserTextView;
    private TextView mLastTweetTextView;

    private final IKeysProvider keysProvider = BloaApp.getKeysProvider();
    private final OAuthConsumer mConsumer =  new CommonsHttpOAuthConsumer(keysProvider.getKey1(), keysProvider.getKey2());

    // You'll need to create this or change the name of DefaultKeysProvider
    IKeysProvider mKeysProvider = new MyKeysProvider();

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.bloa_activity);

        mCB = (CheckBox) this.findViewById(R.id.enable);
        mCB.setChecked(false);
        mCB.setOnClickListener(new LoginCheckBoxClickedListener());

        mEditor = (EditText) this.findViewById(R.id.editor);

        mButton = (Button) this.findViewById(R.id.post);
        mButton.setOnClickListener(new PostButtonClickListener());

        mUserTextView = (TextView) this.findViewById(R.id.user);
        mLastTweetTextView = (TextView) this.findViewById(R.id.last);

        Account account = getAccount();
        if (account == null) {
            AccountManager.get(this).addAccount(Constants.ACCOUNT_TYPE, Constants.AUTHTOKEN_TYPE, null, null, BloaActivity.this, BloaActivity.this, null);
        } else {
            AccountManager.get(BloaActivity.this).getAuthToken(account, Constants.AUTHTOKEN_TYPE, true, BloaActivity.this, null);
        }
        // Set up our cursor loader. It manages the cursors from now on
        getSupportLoaderManager().initLoader(Constants.BLOA_LOADER_ID, null, this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private Account getAccount() {
        AccountManager am = AccountManager.get(this);
        Account[] accounts = am.getAccountsByType(Constants.ACCOUNT_TYPE);
        Assert.assertTrue(accounts.length < 2); // There can only be one: Twitter
        return (accounts.length > 0) ? accounts[0] : null;
    }

    class PostButtonClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            String postString = mEditor.getText().toString();
            if (postString.length() == 0) {
                Toast.makeText(BloaActivity.this, getText(R.string.tweet_empty), Toast.LENGTH_SHORT).show();
            } else {
                ContentValues values = new ContentValues();
                values.put(UserStatusRecord.USER_TEXT, postString);
                values.put(UserStatusRecord.IS_NEW, true);
                values.put(UserStatusRecord.CREATED_DATE, System.currentTimeMillis());
                Assert.assertNotNull(getContentResolver().insert(UserStatusRecords.CONTENT_URI, values));
                mEditor.setText(null);
            }
        }
    }

    private void setLoggedIn(boolean loggedIn) {
        mCB.setChecked(loggedIn);
        mButton.setEnabled(loggedIn);
        mEditor.setEnabled(loggedIn);
        mEditor.setText(null);
        mUserTextView.setText(null);
        this.mLastTweetTextView.setText(null);
        if (! loggedIn) {
            ContentResolver cr = this.getContentResolver();
            cr.delete(UserTimelineRecords.CONTENT_URI, null, null);
            cr.delete(UserStatusRecords.CONTENT_URI, null, null);
        }
        ((FragmentActivity) this).invalidateOptionsMenu();
    }

    class LoginCheckBoxClickedListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            if(!mCB.isChecked()) {
                setLoggedIn(false);
            }
        }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle savedValues) {
        // Create a CursorLoader that will take care of creating a cursor for the data
        return new CursorLoader(this, UserStatusRecords.CONTENT_URI, Constants.USER_STATUS_PROJECTION,
                null, null, UserStatusRecord.DEFAULT_SORT_ORDER);
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.refresh_timeline);
        item.setEnabled(getAccount() != null);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.refresh_timeline:
            ContentResolver.requestSync(getAccount(), BloaProvider.AUTHORITY, new Bundle());
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * This is theAccountManagerCallback. The future result is ready because we waited for it.
     */
    @SuppressWarnings("deprecation")
    @Override
    public void run(AccountManagerFuture<Bundle> futureResult) {
        Log.d(TAG, "Got a future!");
        String token = null;
        String secret = null;
        String type = null;
        if (futureResult.isCancelled()) {
            Log.d(TAG, "Login was canceled");
            return;
        }
        try {
            // We should have an account now
            Account account = getAccount();
            Assert.assertNotNull(account);

            // We're doing this because we need the authtoken when that gets delivered.
            // This call will block until the future is ready. It's ready now
            Bundle authResult = futureResult.getResult();

            type = authResult.getString(AccountManager.KEY_ACCOUNT_TYPE);
            Assert.assertEquals(Constants.ACCOUNT_TYPE, type);

            token = authResult.getString(AccountManager.KEY_ACCOUNT_NAME);
            Assert.assertNotNull(token); // Sanity check
            Log.d(TAG, "token is: " + token);

            // This only gets delivered when on the getAuthtoken result
            secret = authResult.getString(AccountManager.KEY_AUTHTOKEN);
            if (secret != null) {
                Log.d(TAG, "secret is: " + secret);
                // This is the key here
                mConsumer.setTokenWithSecret(token, secret);
            } else {
                AccountManager.get(BloaActivity.this).getAuthToken(account, Constants.AUTHTOKEN_TYPE, true, BloaActivity.this, null);
            }
            setLoggedIn(mConsumer.getToken() != null && mConsumer.getTokenSecret() != null);
        } catch (OperationCanceledException e) {
            e.printStackTrace();
        } catch (AuthenticatorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
