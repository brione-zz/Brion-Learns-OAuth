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
package com.eyebrowssoftware.bloa;

import junit.framework.Assert;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;


public class App extends Application {
    static final String TAG = Constants.class.toString();


    public static void saveRequestInformation(SharedPreferences settings, String token, String secret) {
        // null means to clear the old values
        SharedPreferences.Editor editor = settings.edit();
        if(token == null) {
            editor.remove(Constants.REQUEST_TOKEN);
            Log.d(TAG, "Clearing Request Token");
        }
        else {
            editor.putString(Constants.REQUEST_TOKEN, token);
            Log.d(TAG, "Saving Request Token: " + token);
        }
        if (secret == null) {
            editor.remove(Constants.REQUEST_SECRET);
            Log.d(TAG, "Clearing Request Secret");
        }
        else {
            editor.putString(Constants.REQUEST_SECRET, secret);
            Log.d(TAG, "Saving Request Secret: " + secret);
        }
        editor.commit();

    }

    public static void saveAuthInformation(SharedPreferences settings, String token, String secret) {
        // null means to clear the old values
        SharedPreferences.Editor editor = settings.edit();
        if(token == null) {
            editor.remove(Constants.USER_TOKEN);
            Log.d(TAG, "Clearing OAuth Token");
        }
        else {
            editor.putString(Constants.USER_TOKEN, token);
            Log.d(TAG, "Saving OAuth Token: " + token);
        }
        if (secret == null) {
            editor.remove(Constants.USER_SECRET);
            Log.d(TAG, "Clearing OAuth Secret");
        }
        else {
            editor.putString(Constants.USER_SECRET, secret);
            Log.d(TAG, "Saving OAuth Secret: " + secret);
        }
        editor.commit();

    }

    private OAuthConsumer mConsumer = null;
    private OAuthProvider mProvider = null;

    private KeysProvider mKeysProvider = new DefaultKeysProvider();

    private KeysProvider getKeysProvider() {
        return mKeysProvider;
    }

    public void setKeysProvider(KeysProvider kp) {
        mKeysProvider = kp;
    }

    public OAuthConsumer getOAuthConsumer() {
        return mConsumer;
    }

    public OAuthProvider getOAuthProvider() {
        return mProvider;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Only works with my code unless you create your own MyKeysProvider class
        this.setKeysProvider(new MyKeysProvider());

        mConsumer = new CommonsHttpOAuthConsumer(
                getKeysProvider().getKey1(),
                getKeysProvider().getKey2());

        mProvider = new CommonsHttpOAuthProvider(
            Constants.TWITTER_REQUEST_TOKEN_URL,
            Constants.TWITTER_ACCESS_TOKEN_URL,
            Constants.TWITTER_AUTHORIZE_URL);

        Assert.assertNotNull(mConsumer);
        Assert.assertNotNull(mProvider);

        mProvider.setOAuth10a(true);
    }
}
