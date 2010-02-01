package com.example.bloa;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public final class Statuses {
	
	private Statuses() {}
	
	public static final class Status implements BaseColumns {
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://com.example.bloa/statuses");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of breweries.
         */
        public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.bloa.status";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
         */
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.bloa.status";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "_id ASC";

        /**
         * The id of the Status record
         * <P>Type: LONG
         */
        public static final String _ID = BaseColumns._ID;
        
        /**
         * The text of the status record
         * <P>Type: TEXT</P>
         */
        public static final String TEXT = "text";

        /**
         * The source of the status
         * <P>Type: TEXT</P>
         */
        public static final String SOURCE = "source";

        /**
         * The User Object of the User who made the status
         * <P>Type: TEXT</P>
         */
        public static final String USER = "user";
}

}
