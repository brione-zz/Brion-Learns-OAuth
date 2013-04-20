/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.eyebrowssoftware.bloa.authenticator;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.eyebrowssoftware.bloa.BloaApp;
import com.eyebrowssoftware.bloa.Constants;
import com.eyebrowssoftware.bloa.R;
import com.eyebrowssoftware.bloa.activities.BloaActivity;
import com.eyebrowssoftware.bloa.activities.OAuthActivity;

public class BloaAuthenticator extends AbstractAccountAuthenticator {
    static final String TAG = "BloaAuthenticator";

    private Context mContext;
    private OAuthConsumer mConsumer;

    public BloaAuthenticator(Context context) {
        super(context);
        mContext = context;

        mConsumer = new CommonsHttpOAuthConsumer(
                BloaApp.sKeysProvider.getKey1(),
                BloaApp.sKeysProvider.getKey2());

    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response,
            String accountType, String authTokenType,
            String[] requiredFeatures, Bundle options)
            throws NetworkErrorException {

        Log.d(TAG, "Add Account");
        final Intent intent = new Intent(mContext, OAuthActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response,
            Account account, Bundle options) throws NetworkErrorException {

        Log.d(TAG, "Confirm Credentials");
        return null;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response,
            String accountType) {
        Log.d(TAG, "Edit Properties");
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response,
            Account account, String authTokenType, Bundle options)
            throws NetworkErrorException {

        Log.d(TAG, "Get Auth Token");
        // If the caller requested an authToken type we don't support, then
        // return an error
        if (!authTokenType.equals(Constants.AUTHTOKEN_TYPE)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            return result;
        }

        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        final AccountManager am = AccountManager.get(mContext);
        final String password = am.getPassword(account);
        if (password != null) {
            final String authToken = authenticate(account.name, password);
            if (!TextUtils.isEmpty(authToken)) {
                final Bundle result = new Bundle();
                result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
                result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
                return result;
            }
        }

        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity panel.
        final Intent intent = new Intent(mContext, BloaActivity.class);
        intent.putExtra(Constants.PARAM_USERNAME, account.name);
        intent.putExtra(Constants.PARAM_AUTHTOKEN_TYPE, authTokenType);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return mContext.getResources().getString(R.string.bloa_tweet_authenticator_label);
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response,
            Account account, String[] features) throws NetworkErrorException {

        Log.d(TAG, "Has Features");
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response,
            Account account, String authTokenType, Bundle options)
            throws NetworkErrorException {

        Log.d(TAG, "Update credentials");
        return null;
    }

    class GetCredentialsTask extends AsyncTask<Void, Void, Boolean> {

        HttpClient mClient = BloaApp.getHttpClient();
        @Override
        protected Boolean doInBackground(Void... arg0) {
            HttpGet get = new HttpGet(Constants.VERIFY_URL_STRING);
            try {
                mConsumer.sign(get);
                mClient.execute(get, new BasicResponseHandler());
                return true;
            } catch (Exception e) {
                // Expected if we don't have the proper credentials saved away
                return false;
            }
        }
    }

    private String authenticate(String token, String secret) {
        mConsumer.setTokenWithSecret(token, secret);
        HttpClient mClient = BloaApp.getHttpClient();
        HttpGet get = new HttpGet(Constants.VERIFY_URL_STRING);
        try {
            mConsumer.sign(get);
            mClient.execute(get, new BasicResponseHandler());
            return mConsumer.getTokenSecret();
        } catch (Exception e) {
            return null;
        }
    }

}
