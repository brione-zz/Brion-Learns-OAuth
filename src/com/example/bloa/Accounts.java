package com.example.bloa;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public final class Accounts {
	
	private Accounts() {}
	
	public static final class Account implements BaseColumns {
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://com.example.bloa/accounts");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of accounts.
         */
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.bloa.account";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
         */
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.bloa.account";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "name ASC";

        /**
         * The id of the Status record
         * <P>Type: LONG
         */
        public static final String _ID = BaseColumns._ID;
        
        /**
         * The name of the user
         * <P>Type: TEXT</P>
         */
        public static final String NAME = "name";

        /**
         * The previous posted status of the account
         * <P>Type: TEXT</P>
         */
        public static final String STATUS = "status";

	}

}
