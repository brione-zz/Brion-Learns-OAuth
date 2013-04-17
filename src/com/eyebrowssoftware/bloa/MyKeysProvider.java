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

public class MyKeysProvider implements KeysProvider {
    private static final String TWITTER_CONSUMER_KEY = "naq35tFq1sGd6FxEGTGqjw";
    private static final String TWITTER_CONSUMER_SECRET = "EXa8UfaaW1zD2f7dyRyXuaTuUV3wRvK9UB5nOLGg";

    @Override
    public String getKey1() {
        return TWITTER_CONSUMER_KEY;
    }

    @Override
    public String getKey2() {
        // TODO: Set this to your App's Consumer secret
        return TWITTER_CONSUMER_SECRET;
    }
}
