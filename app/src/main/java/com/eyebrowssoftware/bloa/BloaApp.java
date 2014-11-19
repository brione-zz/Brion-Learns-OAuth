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

import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.Application;
import android.webkit.CookieSyncManager;


public class BloaApp extends Application {
    static final String TAG = "App";

    /**
     * Configures the httpClient to connect to the URL provided.
     */
    public static HttpClient getHttpClient() {
        HttpClient httpClient = new DefaultHttpClient();
        final HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, Constants.HTTP_REQUEST_TIMEOUT_MS);
        HttpConnectionParams.setSoTimeout(params, Constants.HTTP_REQUEST_TIMEOUT_MS);
        ConnManagerParams.setTimeout(params, Constants.HTTP_REQUEST_TIMEOUT_MS);
        return httpClient;
    }

    public static IKeysProvider sKeysProvider = new MyKeysProvider();

    public static IKeysProvider getKeysProvider() {
        return sKeysProvider;
    }

    public static void setKeysProvider(IKeysProvider provider) {
        sKeysProvider = provider;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CookieSyncManager.createInstance(this.getApplicationContext());
    }
}
