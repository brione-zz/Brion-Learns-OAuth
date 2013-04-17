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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import junit.framework.Assert;
import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import android.accounts.AccountAuthenticatorActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eyebrowssoftware.bloa.App;
import com.eyebrowssoftware.bloa.Constants;
import com.eyebrowssoftware.bloa.R;

public class OAuthActivity extends AccountAuthenticatorActivity {
    private static final String TAG = OAuthActivity.class.toString();

    SharedPreferences mSettings;
    OAuthProvider mProvider;
    OAuthConsumer mConsumer;
    Intent mIntent;
    App mApp;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.progress_view);

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);

        mApp = (App) this.getApplication();
        mProvider = mApp.getOAuthProvider();
        mConsumer = mApp.getOAuthConsumer();
        Assert.assertNotNull(mProvider);
        Assert.assertNotNull(mConsumer);

        mIntent = this.getIntent();
        if (mIntent.getData() == null) {
            try {
                (new RetrieveRequestTokenTask()).execute(new Void[0]);
            } catch (Exception e) {
                Log.e(TAG, "OAuthException: " + e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log.d(TAG, "onNewIntent() called");
        Uri uri = intent.getData();
        if (uri != null) {
            // Get the stuff we saved in the async task so we can confirm that it all matches up

            String token = mSettings.getString(Constants.REQUEST_TOKEN, null);
            String secret = mSettings.getString(Constants.REQUEST_SECRET, null);

            // Intent i = new Intent(this, BloaActivity.class); // Currently how we get back to the main activity

            if (token == null || secret == null) {
                throw new IllegalStateException("We should have saved!");
            }
            String otoken = uri.getQueryParameter(OAuth.OAUTH_TOKEN);
            if (otoken != null) {
                // This is a sanity check which should never fail - hence the assertion
                Assert.assertEquals(otoken, mConsumer.getToken());

                // We send out and save the request token, but the secret is not the same as the verifier
                // Apparently, the verifier is decoded to get the secret, which is then compared - crafty
                String verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);

                // We do this in a task now or get an automatic crash
                (new RetrieveAccessTokenTask()).execute(verifier);
            } else {
                String denied = uri.getQueryParameter("denied");
                Log.e(TAG, "Access denied or canceled. Token returned is: " + denied);
                finish();
            }
        }
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
                App.saveRequestInformation(mSettings, mConsumer.getToken(), mConsumer.getTokenSecret());
                OAuthActivity.this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        }
    }

    // This is new and required - we can't be decoding the tokens on the UI thread anymore
    private class RetrieveAccessTokenTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... verifiers) {
            try {
                // This is the moment of truth - we could throw here
                mProvider.retrieveAccessToken(mConsumer, verifiers[0]);
                // Now we can retrieve the goodies
                String token = mConsumer.getToken();
                String secret = mConsumer.getTokenSecret();
                // These are the users token and secret for your app - protect them
                App.saveAuthInformation(mSettings, token, secret);
                // Clear the request stuff, now that we have the real thing
                App.saveRequestInformation(mSettings, null, null);
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
        protected void onPostExecute(Void nada) {
            super.onPostExecute(nada);
            finish();
        }
    }

}
