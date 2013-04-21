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
package com.eyebrowssoftware.bloa.data;

import android.content.ContentResolver;
import android.net.Uri;

import com.eyebrowssoftware.bloa.data.UserStatusRecords.UserStatusRecord;

/**
 * @author brionemde
 *
 */
public final class UserTimelineRecords {

    // Private constructor - This class cannot be instantiated
    private UserTimelineRecords() {
    }

    /**
     * The content:// style URL for this table
     */
    public static final Uri CONTENT_URI =
        BloaProvider.CONTENT_URI.buildUpon().appendPath(BloaProvider.USER_TIMELINE_PATH).build();

    /**
     * The MIME type of {@link #CONTENT_URI} providing a directory of
     * breweries.
     */
    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
        + "/vnd.com.eyebrowssoftware.bloa.user_timeline_record";

    /**
     * @author brionemde
     *
     */
    public static class UserTimelineRecord extends UserStatusRecord {
        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * note.
         */
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
                + "/vnd.com.eyebrowssoftware.bloa.user_status_record";
    }
}
