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
import android.provider.BaseColumns;

/**
 * @author brionemde
 *
 */
public final class UserStatusRecords {

    // Private constructor - This class cannot be instantiated
    private UserStatusRecords() {
    }

    /**
     * The content:// style URL for this table
     */
    public static final Uri CONTENT_URI =
        BloaProvider.CONTENT_URI.buildUpon().appendPath(BloaProvider.USER_STATUS_PATH).build();

    /**
     * The MIME type of {@link #CONTENT_URI} providing a directory of
     * breweries.
     */
    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
        + "/vnd.com.eyebrowssoftware.bloa.user_status_record";

    /**
     * @author brionemde
     *
     */
    public static class UserStatusRecord implements BaseColumns {
        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * note.
         */
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
                + "/vnd.com.eyebrowssoftware.bloa.user_status_record";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = UserStatusRecord.CREATED_DATE + " ASC";

        public static final String _ID = BaseColumns._ID;
        public static final String RECORD_ID = "user_id";
        public static final String USER_NAME = "user_name";
        public static final String USER_TEXT = "user_text";
        public static final String USER_CREATED_DATE = "user_created_date";
        public static final String CREATED_DATE = "created_at";
        public static final String IS_NEW = "is_new";
    }
}
