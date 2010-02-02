package com.example.bloa;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
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
	
	public static final String VERIFY_URL_STRING = "http://twitter.com/account/verify_credentials.json";
	public static final String PUBLIC_TIMELINE_URL_STRING = "http://twitter.com/statuses/public_timeline.json";
	public static final String USER_TIMELINE_URL_STRING = "http://twitter.com/statuses/user_timeline.json";
	public static final String HOME_TIMELINE_URL_STRING = "http://api.twitter.com/1/statuses/home_timeline.json";	
	public static final String FRIENDS_TIMELINE_URL_STRING = "http://api.twitter.com/1/statuses/friends_timeline.json";	
	public static final String STATUSES_URL_STRING = "http://twitter.com/statuses/update.json";	

	ProgressDialog postDialog = null;

	public static final String TWITTER_REQUEST_TOKEN_URL = "http://twitter.com/oauth/request_token";
	public static final String TWITTER_ACCESS_TOKEN_URL = "http://twitter.com/oauth/access_token";
	public static final String TWITTER_AUTHORIZE_URL = "http://twitter.com/oauth/authorize";

	private OAuthConsumer mConsumer = null;
	
	public String mToken;
	public String mSecret;
	
	SharedPreferences mSettings;
	
	MyArrayAdapter mAA;

	LinkedList<UserStatus> mHomeStatus = new LinkedList<UserStatus>();
	
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
		mSettings = getSharedPreferences(OAUTH.PREFS, Context.MODE_PRIVATE);
		mConsumer = new CommonsHttpOAuthConsumer(
				Keys.TWITTER_CONSUMER_KEY, 
				Keys.TWITTER_CONSUMER_SECRET);
		mAA = new MyArrayAdapter(this, android.R.layout.two_line_list_item, android.R.id.text1, mHomeStatus);
		this.setListAdapter(mAA);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// We look for saved user keys
		if(mSettings.contains(OAUTH.USER_TOKEN) && mSettings.contains(OAUTH.USER_SECRET)) {
			mToken = mSettings.getString(OAUTH.USER_TOKEN, null);
			mSecret = mSettings.getString(OAUTH.USER_SECRET, null);
			if(!(mToken == null || mSecret == null)) {
				mConsumer.setTokenWithSecret(mToken, mSecret);
			}
		}
		new GetCredentialsTask().execute();
	}
	
	@Override
	public void onClick(View v) {
		if(mCB.equals(v)) {
			if(mCB.isChecked()) {
				Intent i = new Intent(this, OAUTH.class);
				startActivity(i);
			} else {
				OAUTH.saveAuthInformation(mSettings, null, null);
				mButton.setEnabled(false);
				mEditor.setEnabled(false);
				mCB.setChecked(false);
				mUser.setText("");
			}
			mCB.setChecked(false); // the oauth callback will set it to the proper state
		} else if(mButton.equals(v)) {
			String postString = mEditor.getText().toString();
			if (postString.length() == 0) {
				Toast.makeText(this, getText(R.string.tweet_empty), Toast.LENGTH_SHORT).show();
			} else {
				new PostTask().execute(postString);
			}
		}
	}
	
	private class MyArrayAdapter extends ArrayAdapter<UserStatus> {
		
		LayoutInflater mInflater;
		
		public MyArrayAdapter(Context context, int resource, int textViewResourceId, List<UserStatus> objects) {
			super(context, resource, textViewResourceId, objects);
            mInflater = LayoutInflater.from(context);
		}
		
		@Override
		public View getView(int pos, View reUse, ViewGroup parent) {
			String t;
			ViewHolder holder;
			if(reUse == null) {
				reUse = mInflater.inflate(android.R.layout.two_line_list_item, null);
				holder = new ViewHolder();
				holder.text1 = (TextView) reUse.findViewById(android.R.id.text1);
				holder.text2 = (TextView) reUse.findViewById(android.R.id.text2);
				reUse.setTag(holder);
			} else {
				holder = (ViewHolder) reUse.getTag();
			}
			UserStatus us = this.getItem(pos);
			t = us.getCreatedAt();
			holder.text1.setText(us.getUserName() + ", " + t);
			holder.text2.setText(us.getText());
			return reUse;
		}
	
        private class ViewHolder {
            TextView text1;
            TextView text2;
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
	
	//----------------------------
	// This task is run on every onResume(), to make sure the current credentials are valid.
	// This is probably overkill for a non-educational program
	private class GetCredentialsTask extends AsyncTask<Void, Void, JSONObject> {
 
		ProgressDialog authDialog;
		DefaultHttpClient mClient;
 
		@Override
		protected void onPreExecute() {
			mClient = new DefaultHttpClient();
			authDialog = ProgressDialog.show(BLOA.this, 
					getText(R.string.auth_progress_title), 
					getText(R.string.auth_progress_text), 
					true,	// indeterminate duration
					false); // not cancel-able
		}
 
		@Override
		protected JSONObject doInBackground(Void... arg0) {
			JSONObject jso = null;
	    	HttpGet get = new HttpGet(VERIFY_URL_STRING);
	    	try {
				mConsumer.sign(get);
				String response = mClient.execute(get, new BasicResponseHandler());
				jso = new JSONObject(response);
				Log.d(TAG, "authenticatedQuery: " + jso.toString(2));
			} catch (OAuthMessageSignerException e) {
				e.printStackTrace();
			} catch (OAuthExpectationFailedException e) {
				e.printStackTrace();
			} catch (OAuthCommunicationException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return jso;
		}
 
		// This is in the UI thread, so we can mess with the UI
		protected void onPostExecute(JSONObject jso) {
			mClient.getConnectionManager().shutdown();
			authDialog.dismiss();
			mCB.setChecked(jso != null);
			mButton.setEnabled(jso != null);
			mEditor.setEnabled(jso != null);
			mUser.setText(jso != null ? getUserName(jso) : getString(R.string.userhint));
			mLast.setText(jso != null ? getLastTweet(jso) : getString(R.string.userhint));
			if(jso != null) {
				TimelineSelector ss = new TimelineSelector(HOME_TIMELINE_URL_STRING);
				new GetTimelineTask().execute(ss);
			}
		}
	}
 
	//----------------------------
	// This task posts a message to your message queue on the service.
	private class PostTask extends AsyncTask<String, Void, JSONObject> {
 
		ProgressDialog postDialog;
		DefaultHttpClient mClient;
 
		@Override
		protected void onPreExecute() {
			mClient = new DefaultHttpClient();
			postDialog = ProgressDialog.show(BLOA.this, 
					getText(R.string.tweet_progress_title), 
					getText(R.string.tweet_progress_text), 
					true,	// indeterminate duration
					false); // not cancel-able
		}
 
		@Override
		protected JSONObject doInBackground(String... params) {
 
			JSONObject jso = null;
			try {
				HttpPost post = new HttpPost("http://twitter.com/statuses/update.json");
				LinkedList<BasicNameValuePair> out = new LinkedList<BasicNameValuePair>();
				out.add(new BasicNameValuePair("status", params[0]));
				post.setEntity(new UrlEncodedFormEntity(out, HTTP.UTF_8));
				post.setParams(getParams());
				// sign the request to authenticate
				mConsumer.sign(post);
				String response = mClient.execute(post, new BasicResponseHandler());
				jso = new JSONObject(response);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (OAuthMessageSignerException e) {
				e.printStackTrace();
			} catch (OAuthExpectationFailedException e) {
				e.printStackTrace();
			} catch (OAuthCommunicationException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} finally {
 
			}
			return jso;
		}
 
		// This is in the UI thread, so we can mess with the UI
		protected void onPostExecute(JSONObject jso) {
			mClient.getConnectionManager().shutdown();
			postDialog.dismiss();
			if(jso != null) { // authorization succeeded, the json object contains the user information
				mEditor.setText("");
				mLast.setText(getCurrentTweet(jso));
			} else {
				mLast.setText(getText(R.string.tweet_error));
			}
		}
	}
	
	
	private class TimelineSelector extends Object {
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
	
	private class UserStatus {
		
		JSONObject mStatus;
		JSONObject mUser;
		
		public UserStatus(JSONObject status) throws JSONException {

			mStatus = status;
			mUser = status.getJSONObject("user");
		}
		public long getId() {
			return mStatus.optLong("id", -1);
		}
		public String getUserName() {
			return mUser.optString("name", getString(R.string.bad_value));
		}
		public String getText() {
			return getCurrentTweet(mStatus);
		}
		public String getCreatedAt() {
			Time ret1 = new Time();
			return mStatus.optString("created_at", getString(R.string.bad_value));
		}
	}
	
	private class GetTimelineTask extends AsyncTask<TimelineSelector, Void, JSONArray> {

		DefaultHttpClient mClient;
		
		@Override
		protected void onPreExecute() {
			mClient = new DefaultHttpClient();
		}

		@Override
		protected JSONArray doInBackground(TimelineSelector... params) {
			JSONArray array = null;
			try {
				for(int i = 0; i < params.length; ++i) {
					Uri sUri = Uri.parse(params[i].url);
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
					HttpGet get = new HttpGet(builder.build().toString());
					mConsumer.sign(get);
					String response = mClient.execute(get, new BasicResponseHandler());
					array = new JSONArray(response);
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
			return array;
		}

		// This is in the UI thread, so we can mess with the UI
		protected void onPostExecute(JSONArray array) {
			mClient.getConnectionManager().shutdown();
			if(array != null) {
				try {
					for(int i = 0; i < array.length(); ++i) {
						JSONObject status = array.getJSONObject(i);
						UserStatus s = new UserStatus(status);
						mHomeStatus.add(s);
					}
					mAA.notifyDataSetChanged();
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
			}
		}
	}
}