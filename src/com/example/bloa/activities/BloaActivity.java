package com.example.bloa.activities;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

import oauth.signpost.OAuthConsumer;

import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bloa.App;
import com.example.bloa.KeysProvider;
import com.example.bloa.MyKeysProvider;
import com.example.bloa.R;
import com.example.bloa.data.UserStatusRecords;
import com.example.bloa.data.UserStatusRecords.UserStatusRecord;

public class BloaActivity extends FragmentActivity implements LoaderCallbacks<Cursor> {
    public static final String TAG = "BLOA";

    private CheckBox mCB;
    private EditText mEditor;
    private Button mButton;
    private TextView mUser;
    private TextView mLast;

    private OAuthConsumer mConsumer = null;

    private String mToken;
    private String mSecret;

    private SharedPreferences mSettings;


    // You'll need to create this or change the name of DefaultKeysProvider
    KeysProvider mKeysProvider = new MyKeysProvider();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        mConsumer = ((App) getApplication()).getOAuthConsumer();

        mCB = (CheckBox) this.findViewById(R.id.enable);
        mCB.setChecked(false);
        mCB.setOnClickListener(new LoginCheckBoxClickedListener());

        mEditor = (EditText) this.findViewById(R.id.editor);

        mButton = (Button) this.findViewById(R.id.post);
        mButton.setOnClickListener(new PostButtonClickListener());

        mUser = (TextView) this.findViewById(R.id.user);
        mLast = (TextView) this.findViewById(R.id.last);

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);

        getSupportLoaderManager().initLoader(App.BLOA_LOADER_ID, null, (LoaderCallbacks<Cursor>) this);
}

    @Override
    public void onResume() {
        super.onResume();

        // We look for saved user keys
        if(mSettings.contains(App.USER_TOKEN) && mSettings.contains(App.USER_SECRET)) {
            mToken = mSettings.getString(App.USER_TOKEN, null);
            mSecret = mSettings.getString(App.USER_SECRET, null);
            // If we find some we update the consumer with them
            if(!(mToken == null || mSecret == null)) {
                mConsumer.setTokenWithSecret(mToken, mSecret);
            }
        }
        (new GetCredentialsTask()).execute();
    }

    //----------------------------
    // This task is run on every onResume(), to make sure the current credentials are valid.
    // This is probably overkill for a non-educational program
    class GetCredentialsTask extends AsyncTask<Void, Void, Boolean> {

        ProgressDialog authDialog;

        @Override
        protected void onPreExecute() {
            authDialog = ProgressDialog.show(BloaActivity.this,
                getText(R.string.auth_progress_title),
                getText(R.string.auth_progress_text),
                true,   // indeterminate duration
                false); // not cancelable
        }

        @Override
        protected Boolean doInBackground(Void... arg0) {
            JSONObject jso = null;
            HttpsURLConnection connection = null;
            try {
                URL url = new URL(App.VERIFY_URL_STRING);
                connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
                mConsumer.sign(connection);
                connection.connect();
                int code = connection.getResponseCode();
                Log.d(TAG,  "Response code: " + code);
                if (code != 200) {
                    return false;
                }
                String jsonResponse = readInput(connection.getInputStream());
                Log.d(TAG, "Auth response: " + jsonResponse);
                jso = new JSONObject(jsonResponse);
                makeNewUserStatusRecord(jso);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Exception confirming user", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return false;
        }

        // This is in the UI thread, so we can mess with the UI
        @Override
        protected void onPostExecute(Boolean loggedIn) {
            authDialog.dismiss();
            mCB.setChecked(loggedIn);
            mButton.setEnabled(loggedIn);
            mEditor.setEnabled(loggedIn);
            if (loggedIn) {
                TimelineSelector ss = new TimelineSelector(App.HOME_TIMELINE_URL_STRING);
                new GetTimelineTask().execute(ss);
            }
        }
    }

    private String readInput(InputStream is) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            sb.append(line);
        }
        in.close();
        return sb.toString();
    }

    private void makeNewUserStatusRecord(JSONObject object) {
        // Delete any existing records for user
        getContentResolver().delete(UserStatusRecords.CONTENT_URI, App.USER_STATUS_QUERY_WHERE, null);
        try {
            ContentValues values = new ContentValues();
            values.put(UserStatusRecord.USER_NAME, object.getString("name"));
            values.put(UserStatusRecord.RECORD_ID, object.getInt("id_str"));
            values.put(UserStatusRecord.USER_CREATED_DATE, object.getString("created_at"));
            JSONObject status = object.getJSONObject("status");
            values.put(UserStatusRecord.USER_TEXT, status.getString("text"));
            values.put(UserStatusRecord.LATEST_STATUS, "true");
            getContentResolver().insert(UserStatusRecords.CONTENT_URI, values);
            Log.d(TAG, "makeNewUserStatusRecord: " + values.toString());
        } catch (Exception e) {
            Log.e(TAG, "Exception adding users status record", e);
        }
    }

    class PostButtonClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            String postString = mEditor.getText().toString();
            if (postString.length() == 0) {
                Toast.makeText(BloaActivity.this, getText(R.string.tweet_empty), Toast.LENGTH_SHORT).show();
            } else {
                new PostTask().execute(postString);
            }
        }
    }

    class LoginCheckBoxClickedListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            if(mCB.isChecked()) {
                BloaActivity.this.startActivity(new Intent(BloaActivity.this, OAuthActivity.class));
            } else {
                App.saveAuthInformation(mSettings, null, null);
                mButton.setEnabled(false);
                mEditor.setEnabled(false);
                mCB.setChecked(false);
                mUser.setText("");
            }
            mCB.setChecked(false); // the oauth callback will set it to the proper state
        }
    }

    //----------------------------
    // This task posts a message to your message queue on the service.
    class PostTask extends AsyncTask<String, Void, JSONObject> {

        ProgressDialog postDialog;

        @Override
        protected void onPreExecute() {
            postDialog = ProgressDialog.show(BloaActivity.this,
                    getText(R.string.tweet_progress_title),
                    getText(R.string.tweet_progress_text),
                    true,	// indeterminate duration
                    false); // not cancel-able
        }

        @Override
        protected JSONObject doInBackground(String... params) {

            JSONObject jso = null;
            HttpsURLConnection connection = null;
            try {

                URL url = new URL(App.STATUSES_URL_STRING);

                connection = (HttpsURLConnection) url.openConnection();

                connection.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
                connection.setDoOutput(true);

                mConsumer.sign(connection);

                //Send request
                OutputStream wr = connection.getOutputStream ();
                wr.write(("status=" + params[0].substring(0, Math.min(params[0].length(), 139))).getBytes("UTF-8"));
                wr.flush();
                // sign the request to authenticate
                connection.connect();
                int status = connection.getResponseCode();
                Log.d(TAG, "Post Message response: " + status);
                if (status == 200) {
                    InputStream in = new BufferedInputStream(connection.getInputStream());
                    String stuff = in.toString();
                    jso = new JSONObject(stuff);
                }
            } catch (Exception e) {
                Log.e(TAG, "Post Task Exception", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return jso;
        }

        // This is in the UI thread, so we can mess with the UI
        protected void onPostExecute(JSONObject jso) {
            postDialog.dismiss();
            if(jso != null) { // authorization succeeded, the json object contains the user information
                mEditor.setText("");
                // XXX: Fix this mLast.setText(UserStatus.getCurrentTweet(jso));
            } else {
                mLast.setText(getText(R.string.tweet_error));
            }
        }
    }


    class TimelineSelector extends Object {
        public String url; // the url to perform the query from
        // not all these apply to every url - you are responsible
        public Long since_id; // ids newer than this will be fetched
        public Long max_id; // ids older than this will be fetched
        public Integer count; // # of tweets to fetch Max is 200
        public Integer page; // # of page to fetch (with limits)

        public TimelineSelector(String u) {
            url = u;
            max_id = null;
            since_id = null;
            count = null;
            page = null;
        }

        public TimelineSelector(String u, Long since, Long max, Integer cnt, Integer pg) {
            url = u;
            max_id = max;
            since_id = since;
            count = cnt;
            page = pg;
        }
    }


    class GetTimelineTask extends AsyncTask<TimelineSelector, Void, JSONArray> {


        @Override
        protected JSONArray doInBackground(TimelineSelector... params) {
            JSONArray array = null;
            HttpsURLConnection connection = null;
            try {
                Uri sUri = Uri.parse(params[0].url);
                Uri.Builder builder = sUri.buildUpon();
                if(params[0].since_id != null) {
                    builder.appendQueryParameter("since_id", String.valueOf(params[0].since_id));
                } else if (params[0].max_id != null) { // these are mutually exclusive
                    builder.appendQueryParameter("max_id", String.valueOf(params[0].max_id));
                }
                if(params[0].count != null) {
                    builder.appendQueryParameter("count", String.valueOf((params[0].count > 200) ? 200 : params[0].count));
                }
                if(params[0].page != null) {
                    builder.appendQueryParameter("page", String.valueOf(params[0].page));
                }
                URL url = new URL(builder.build().toString());
                connection = (HttpsURLConnection) url.openConnection();
                mConsumer.sign(connection);
                connection.connect();
                String jsonResponse = readInput(connection.getInputStream());
                Log.d(TAG, "Auth response: " + jsonResponse);
                array = new JSONArray(jsonResponse);
                // Delete the existing timeline
                getContentResolver().delete(UserStatusRecords.CONTENT_URI, App.USER_TIMELINE_QUERY_WHERE, null);
                for(int i = 0; i < array.length(); ++i) {
                    JSONObject status = array.getJSONObject(i);
                    Log.d(TAG, status.toString());
                    makeNewUserTimelineRecord(status);
                }
            } catch (Exception e) {
                Log.e(TAG, "Get Timeline Exception", e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return array;
        }
    }

    private void makeNewUserTimelineRecord(JSONObject object) {
        try {
            ContentValues values = new ContentValues();
            JSONObject user = object.getJSONObject("user");
            values.put(UserStatusRecord.USER_NAME, user.getString("name"));
            values.put(UserStatusRecord.RECORD_ID, user.getInt("id_str"));
            values.put(UserStatusRecord.USER_CREATED_DATE, object.getString("created_at"));
            values.put(UserStatusRecord.USER_TEXT, object.getString("text"));
            getContentResolver().insert(UserStatusRecords.CONTENT_URI, values);
            Log.d(TAG, "makeNewUserStatusRecord: " + values.toString());
        } catch (Exception e) {
            Log.e(TAG, "Exception adding users status record", e);
        }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle savedValues) {
        // Create a CursorLoader that will take care of creating a cursor for the data
        return new CursorLoader(this, UserStatusRecords.CONTENT_URI,
            App.USER_STATUS_PROJECTION, App.USER_STATUS_QUERY_WHERE,
            null, UserStatusRecord.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // We got something but it might be empty
        if (cursor.moveToFirst()) {
            mUser.setText(cursor.getString(App.IDX_USER_STATUS_USER_NAME));
            mLast.setText(cursor.getString(App.IDX_USER_STATUS_USER_TEXT));

        } else {
            mUser.setText(getString(R.string.userhint));
            mLast.setText(getString(R.string.userhint));
        }


    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        mUser.setText(getString(R.string.userhint));
        mLast.setText(getString(R.string.userhint));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.bloa_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.refresh_timeline:
            TimelineSelector ss = new TimelineSelector(App.HOME_TIMELINE_URL_STRING);
            new GetTimelineTask().execute(ss);
            return true;
        default:
            return false;
        }
    }
}
