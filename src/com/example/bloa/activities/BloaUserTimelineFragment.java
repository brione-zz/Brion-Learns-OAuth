package com.example.bloa.activities;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;

import com.example.bloa.App;
import com.example.bloa.R;
import com.example.bloa.data.UserStatusRecords;
import com.example.bloa.data.UserStatusRecords.UserStatusRecord;

public class BloaUserTimelineFragment extends ListFragment implements LoaderCallbacks<Cursor> {

    private static final int[] IDS = {
        android.R.id.text1,
        android.R.id.text2
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.setEmptyText(this.getString(R.string.empty));
        // No cursor yet. Will be assigned when the CursorLoader query is complete
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this.getActivity(),
            android.R.layout.simple_list_item_2, null, App.USER_STATUS_PROJECTION, IDS, 0);
        setListAdapter(adapter);
        setListShown(false);
        // Set up our cursor loader. It manages the cursors from now on
        getLoaderManager().initLoader(App.LIST_LOADER_ID, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle arguments) {
        return new CursorLoader(getActivity(), UserStatusRecords.CONTENT_URI,
            App.USER_STATUS_PROJECTION, App.USER_TIMELINE_QUERY_WHERE, null,
            UserStatusRecord.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (this.isResumed()) {
            this.setListShown(true);
        }
        else {
            this.setListShownNoAnimation(true);
        }
        ((SimpleCursorAdapter) this.getListAdapter()).swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        ((SimpleCursorAdapter) this.getListAdapter()).swapCursor(null);
    }
}
