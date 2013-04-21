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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
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

    private Boolean mIsLoggedIn = false;

    // You'll need to create this or change the name of DefaultKeysProvider
    IKeysProvider mKeysProvider = new MyKeysProvider();

    // TODO: decide to move the minSdkVersion forward or not
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        mCR = this.getContentResolver();

        mConsumer = ((BloaApp) getApplication()).getOAuthConsumer();

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
        } else {
            mAm.addAccount(Constants.ACCOUNT_TYPE, Constants.AUTHTOKEN_TYPE, null, null, BloaActivity.this, BloaActivity.this, null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAccount != null) {
            // Assume we're logged in, for now, so we can move some code here
            setLoggedIn(mIsLoggedIn);
         }
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

    private void setLoggedIn(boolean loggedIn) {
        mCB.setChecked(loggedIn);
        mButton.setEnabled(loggedIn);
        mEditor.setEnabled(loggedIn);
        mEditor.setText(null);
        mUserTextView.setText(null);
        this.mLastTweetTextView.setText(null);
    }

    class LoginCheckBoxClickedListener implements OnClickListener {

        @SuppressWarnings("deprecation")
        @Override
        public void onClick(View v) {
            if(mCB.isChecked()) {
                if (mAccount != null) {
                    mAm.getAuthToken(mAccount, Constants.AUTHTOKEN_TYPE, true, BloaActivity.this, null);
                } else {
                    throw new IllegalStateException("You somehow got here without having an account. That button should be inactive right now");
                }
            } else {
                setLoggedIn(false);
            }
            mCB.setChecked(false); // the oauth callback will set it to the proper state
        }
    }

    //----------------------------
    // This task posts a message to your message queue on the service.
    static class PostTask extends AsyncTask<String, Void, Void> {

        HttpClient mClient = BloaApp.getHttpClient();
        OAuthConsumer mConsumer = BloaApp.getOAuthConsumer();

        @Override
        protected Void doInBackground(String... params) {
            try {

                HttpPost post = new HttpPost(Constants.STATUSES_URL_STRING);
                LinkedList<BasicNameValuePair> out = new LinkedList<BasicNameValuePair>();
                out.add(new BasicNameValuePair("status", params[0]));
                post.setEntity(new UrlEncodedFormEntity(out, HTTP.UTF_8));
                // sign the request to authenticate
                mConsumer.sign(post);
                mClient.execute(post, new BasicResponseHandler());
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
            }
            return null;
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
            ContentResolver.requestSync(mAccount, BloaProvider.AUTHORITY, new Bundle());
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
        String type = null;
        try {
            Bundle authResult = futureResult.getResult();

            type = authResult.getString(AccountManager.KEY_ACCOUNT_TYPE);
            Assert.assertEquals(Constants.ACCOUNT_TYPE, type);

            token = authResult.getString(AccountManager.KEY_ACCOUNT_NAME);
            Assert.assertNotNull(token);
            Log.d(TAG, "token is: " + token);
            if (mAccount != null) {
                mAccount = new Account(token, type);
            }
            secret = authResult.getString(AccountManager.KEY_AUTHTOKEN);
            if (secret != null) {
                Log.d(TAG, "secret is: " + secret);
                mConsumer.setTokenWithSecret(token, secret);
                setLoggedIn(mIsLoggedIn = true);
            }
        } catch (OperationCanceledException e) {
            e.printStackTrace();
        } catch (AuthenticatorException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
