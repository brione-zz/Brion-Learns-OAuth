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

import junit.framework.Assert;
import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import com.eyebrowssoftware.bloa.BloaApp;
import com.eyebrowssoftware.bloa.Constants;
import com.eyebrowssoftware.bloa.IKeysProvider;
import com.eyebrowssoftware.bloa.R;
import com.eyebrowssoftware.bloa.data.BloaProvider;

public class OAuthActivity extends AccountAuthenticatorActivity {
    static final String TAG = "OAuthActivity";

    private  OAuthProvider mProvider;
    private  OAuthConsumer mConsumer;
    private Boolean mConfirmCredentials = false;
    private Boolean mRequestNewAccount = false;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mProvider = new CommonsHttpOAuthProvider(
                Constants.TWITTER_REQUEST_TOKEN_URL,
                Constants.TWITTER_ACCESS_TOKEN_URL,
                Constants.TWITTER_AUTHORIZE_URL);

        mProvider.setOAuth10a(true);

        IKeysProvider keys = BloaApp.getKeysProvider();
        mConsumer =  new CommonsHttpOAuthConsumer(keys.getKey1(), keys.getKey2());

        Intent mIntent = this.getIntent();
        String token = mIntent.getStringExtra(Constants.PARAM_USERNAME);
        mRequestNewAccount = token == null;
        mConfirmCredentials = mIntent.getBooleanExtra(Constants.PARAM_CONFIRM_CREDENTIALS, false);

        Log.i(TAG, "    request new: " + mRequestNewAccount);
        requestWindowFeature(Window.FEATURE_LEFT_ICON);

        setContentView(R.layout.progress_view);

        (new RetrieveRequestTokenTask()).execute(new Void[0]);
    }

    // This is new and required - we can't be decoding the tokens on the UI thread anymore
    private class RetrieveRequestTokenTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            String url = null;
            try {

                url = mProvider.retrieveRequestToken(mConsumer, Constants.CALLBACK_URL);

            } catch (OAuthMessageSignerException e) {
                e.printStackTrace();
            } catch (OAuthNotAuthorizedException e) {
                e.printStackTrace();
            } catch (OAuthExpectationFailedException e) {
                e.printStackTrace();
            } catch (OAuthCommunicationException e) {
                e.printStackTrace();
            }
            return url;
        }

        @Override
        protected void onPostExecute(String url) {
            super.onPostExecute(url);
            if (url != null) {
                OAuthActivity.this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        }
    }

    // This is the callback from the browser-based authentication
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Uri uri = intent.getData();
        Assert.assertNotNull(uri);
        String otoken = uri.getQueryParameter(OAuth.OAUTH_TOKEN);
        if (otoken != null) {
            // This is a sanity check which should never fail - hence the assertion
            Assert.assertEquals(otoken, mConsumer.getToken());
            /*
             * We have to do this in a thread now or get an automatic crash
             * because it involves hidden network operations
             */
            String verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);
            (new RetrieveAccessTokenTask()).execute(verifier);
        } else {
            String denied = uri.getQueryParameter("denied");
            Log.i(TAG, String.format("Access denied or canceled. Token returned is %1$s", denied));
            finish();
        }
    }

    // This is new and required - we can't be decoding the tokens on the UI thread anymore
    private class RetrieveAccessTokenTask extends AsyncTask<String, Void, OAuthConsumer> {

        @Override
        protected OAuthConsumer doInBackground(String... verifiers) {
            try {
                // This is the moment of truth - we could throw here
                mProvider.retrieveAccessToken(mConsumer, verifiers[0]);
                return mConsumer;
            } catch (OAuthMessageSignerException e) {
                e.printStackTrace();
            } catch (OAuthNotAuthorizedException e) {
                e.printStackTrace();
            } catch (OAuthExpectationFailedException e) {
                e.printStackTrace();
            } catch (OAuthCommunicationException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(OAuthConsumer consumer) {
            super.onPostExecute(consumer);

            if (consumer != null) {
                onAuthenticationResult(consumer.getToken(), consumer.getTokenSecret());
            } else {
                Toast.makeText(OAuthActivity.this, R.string.account_retry, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }


    private void onAuthenticationResult(String token, String secret) {
        if (!mConfirmCredentials) {
            finishLogin(token, secret);
        } else {
            finishConfirmCredentials(token, secret);
        }
    }

    /**
     * Called when response is received from the server for confirm credentials
     * request. See onAuthenticationResult(). Sets the
     * AccountAuthenticatorResult which is sent back to the caller.
     *
     * @param result the confirmCredentials result.
     */
    private void finishConfirmCredentials(String token, String secret) {
        Log.i(TAG, "finishConfirmCredentials()");
        final Account account = new Account(token, Constants.ACCOUNT_TYPE);
        AccountManager.get(this).setPassword(account, secret);
        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_BOOLEAN_RESULT, true);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }



    /**
     * Called when response is received from the server for authentication
     * request. See onAuthenticationResult(). Sets the
     * AccountAuthenticatorResult which is sent back to the caller. We store the
     * authToken that's returned from the server as the 'password' for this
     * account - so we're never storing the user's actual password locally.
     *
     * @param result the confirmCredentials result.
     */
    private void finishLogin(String token, String secret) {

        Log.i(TAG, "finishLogin()");
        final Account account = new Account(token, Constants.ACCOUNT_TYPE);
        if (mRequestNewAccount) {
            Bundle specific = new Bundle();
            specific.putString(Constants.PARAM_USERNAME, token);
            specific.putString(Constants.PARAM_PASSWORD, null);
            AccountManager.get(this).addAccountExplicitly(account, secret, specific);
            // Set contacts sync for this account.
            ContentResolver.setSyncAutomatically(account, BloaProvider.AUTHORITY, true);
        } else {
            AccountManager.get(this).setPassword(account, null);
        }
        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, token);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
        intent.putExtra(AccountManager.KEY_AUTHTOKEN, secret);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }
}
