package com.example.bloa;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class BLOA extends ListActivity implements OnClickListener {
	public static final String TAG = "BLOA";

	private CheckBox mCB;
	private EditText mEditor;
	private Button mButton;
	private TextView mUser;
	private TextView mLast;
	
	ProgressDialog postDialog = null;

	public static final String USER_TOKEN = "user_token";
	public static final String USER_SECRET = "user_secret";
	public static final String REQUEST_TOKEN = "request_token";
	public static final String REQUEST_SECRET = "request_secret";

	public static final String TWITTER_REQUEST_TOKEN_URL = "http://twitter.com/oauth/request_token";
	public static final String TWITTER_ACCESS_TOKEN_URL = "http://twitter.com/oauth/access_token";
	public static final String TWITTER_AUTHORIZE_URL = "http://twitter.com/oauth/authorize";

	private static final Uri CALLBACK_URI = Uri.parse("bloa-app://twitt");

	private static final String PREFS = "MyPrefsFile";

	private OAuthConsumer mConsumer = null;
	private OAuthProvider mProvider = null;
	
	public String mToken;
	public String mSecret;
	
	private MyAsyncQueryHandler mAQH;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mCB = (CheckBox) this.findViewById(R.id.enable);
		mCB.setChecked(false);
		mEditor = (EditText) this.findViewById(R.id.editor);
		mButton = (Button) this.findViewById(R.id.post);
		mUser = (TextView) this.findViewById(R.id.user);
		mLast = (TextView) this.findViewById(R.id.last);
		mButton.setOnClickListener(this);
		mCB.setOnClickListener(this);
		mAQH = new MyAsyncQueryHandler(getContentResolver());
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// We look for saved user keys
		SharedPreferences settings = this.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
		if(settings.contains(OAUTH.USER_TOKEN) && settings.contains(OAUTH.USER_SECRET)) {
			mToken = settings.getString(OAUTH.USER_TOKEN, null);
			mSecret = settings.getString(OAUTH.USER_SECRET, null);
			if(!(mToken == null || mSecret == null)) {
				Uri sUri = BLOAProvider.CONTENT_URI.buildUpon().appendPath("verify")
					.appendQueryParameter(BLOAProvider.AUTH_TOKEN, mToken)
					.appendQueryParameter(BLOAProvider.AUTH_SECRET, mSecret).build();
				
				String[] proj = { Accounts.Account._ID, Accounts.Account.NAME, Accounts.Account.STATUS };
				mAQH.startQuery(0, TAG, sUri, proj, null, null, null);
			}
		}
	}
	
	private class MyAsyncQueryHandler extends AsyncQueryHandler {

		public MyAsyncQueryHandler(ContentResolver cr) {
			super(cr);
			// TODO Auto-generated constructor stub
		}
		
		protected void onQueryComplete(int token, Object cookie, Cursor c) {
			switch(token) {
			case 0:
				// Verify credentials query
				mCB.setChecked(c != null && c.getCount() > 0);
				mButton.setEnabled(c != null && c.getCount() > 0);
				mEditor.setEnabled(c != null && c.getCount() > 0);
				if (c == null || c.getCount() == 0)
					break;
				c.moveToFirst();
				mUser.setText(c.getString(1));
				String status = c.getString(2);
				if(status == null || status.equals(""))
					break;
				try {
					JSONObject sjso = new JSONObject(status);
					String text = sjso.getString("text");
					mLast.setText(text);
					Log.d(TAG, "Tweet: " + text);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
			
		}
		
		protected void onInsertComplete(int token, Object cookie, Uri uri) {
			if(uri != null) {
				mEditor.setText("");
				mLast.setText("");
			}
		}
	}

	@Override
	public void onClick(View v) {
		if(mCB.equals(v)) {
			if(mCB.isChecked()) {
				OAUTH.saveRequestInformation(this, null, null); // clear any request information
				Intent i = new Intent(this, OAUTH.class);
				startActivity(i);
			} else {
				OAUTH.saveAuthInformation(this, null, null);
				mButton.setEnabled(false);
				mEditor.setEnabled(false);
				mCB.setChecked(false);
				mUser.setText("");
			}
			mCB.setChecked(false); // the oauth callback will set it to the proper state
		} else if(mButton.equals(v)) {
			String postString = mEditor.getText().toString();
			if (postString.length() == 0) {
				Toast.makeText(this, getText(R.string.tweet_empty),
						Toast.LENGTH_SHORT).show();
			} else {
				ContentValues cv = new ContentValues();
				cv.put("status", postString);
				Uri uri = BLOAProvider.CONTENT_URI.buildUpon().appendPath("statuses").build();
				mAQH.startInsert(1, TAG, uri, cv);
			}
		}
	}

	// Get stuff from the two types of Twitter JSONObject we deal with: credentials and status 
	private String getCurrentTweet(JSONObject status) {
		return status.optString("text", getString(R.string.bad_value));
	}

	private String getUserName(JSONObject credentials) {
		return credentials.optString("name", getString(R.string.bad_value));
	}

	private String getLastTweet(JSONObject credentials) {
		try {
			JSONObject status = credentials.getJSONObject("status");
			return getCurrentTweet(status);
		} catch (JSONException e) {
			e.printStackTrace();
			return getString(R.string.tweet_error);
		}
	}

	// These parameters are needed to talk to the messaging service
	public HttpParams getParams() {
		// Tweak further as needed for your app
		HttpParams params = new BasicHttpParams();
		// set this to false, or else you'll get an Expectation Failed: error
		HttpProtocolParams.setUseExpectContinue(params, false);
		return params;
	}
	
	private class StatusSelector extends Object {
		public Long since_id; // ids newer than this will be fetched
		public Long max_id; // ids older than this will be fetched
		public Integer count; // # of tweets to fetch Max is 200
		public Integer page; // # of page to fetch (with limits)
	}
	
	private class GetHomeStatuses extends AsyncTask<StatusSelector, Void, JSONObject> {

		DefaultHttpClient mClient;
		
		@Override
		protected void onPreExecute() {
			mClient = new DefaultHttpClient();
		}

		@Override
		protected JSONObject doInBackground(StatusSelector... params) {
			JSONObject jso = null;
			try {
				for(int i = 0; i < params.length; ++i) {
					Uri sUri = Uri.parse("http://api.twitter.com/1/statuses/home_timeline.json");
					Uri.Builder builder = sUri.buildUpon();
					if(params[i].since_id != null) {
						builder.appendQueryParameter("since_id", String.valueOf(params[i].since_id));
					} else if (params[i].max_id != null) { // these are mutually exclusive
						builder.appendQueryParameter("max_id", String.valueOf(params[i].max_id));
					}
					if(params[i].count != null) {
						builder.appendQueryParameter("count", String.valueOf((params[i].count > 200) ? 200 : params[i].count));
					}
					if(params[i].page != null) {
						builder.appendQueryParameter("page", String.valueOf(params[i].page));
					}
					HttpGet get = new HttpGet(sUri.toString());
					mConsumer.sign(get);
					String response = mClient.execute(get, new BasicResponseHandler());
					jso = new JSONObject(response);
					Log.d(TAG, "Response: " + jso.toString(2));
				}
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (OAuthMessageSignerException e) {
				e.printStackTrace();
			} catch (OAuthExpectationFailedException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (OAuthCommunicationException e) {
				e.printStackTrace();
			}
			return jso;
		}

		// This is in the UI thread, so we can mess with the UI
		protected void onPostExecute(JSONObject jso) {
			mClient.getConnectionManager().shutdown();
			if(jso != null) {
				
			} else {
			}
		}
	}
}