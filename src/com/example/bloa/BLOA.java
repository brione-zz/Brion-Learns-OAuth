package com.example.bloa;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;

import oauth.signpost.OAuth;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;
import oauth.signpost.signature.SignatureMethod;

import org.apache.http.HttpEntity;
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
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class BLOA extends Activity implements OnCheckedChangeListener,
		OnClickListener {
	public static final String TAG = "BLOA";

	public static final String TWITTER_REQUEST_TOKEN_URL = "http://twitter.com/oauth/request_token";
	public static final String TWITTER_ACCESS_TOKEN_URL = "http://twitter.com/oauth/access_token";
	public static final String TWITTER_AUTHORIZE_URL = "http://twitter.com/oauth/authorize";

	private static final Uri CALLBACK_URI = Uri.parse("bloa-app://twitt");

	private static final String PREFS = "MyPrefsFile";

	private OAuthConsumer mConsumer;
	private OAuthProvider mProvider;

	private CheckBox mCB;
	private EditText mEditor;
	private Button mButton;
	private TextView mDisplay;
	private TextView mUser;
	
	ProgressDialog postDialog = null;

	private static final String TOKEN_STRING = "token";
	private static final String SECRET_STRING = "secret";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mCB = (CheckBox) this.findViewById(R.id.enable);
		mCB.setChecked(false);
		mEditor = (EditText) this.findViewById(R.id.editor);
		mButton = (Button) this.findViewById(R.id.post);
		mDisplay = (TextView) this.findViewById(R.id.last);
		mUser = (TextView) this.findViewById(R.id.user);

		if (savedInstanceState != null) {
			mConsumer = (OAuthConsumer) savedInstanceState
					.getSerializable("consumer");
			mProvider = (OAuthProvider) savedInstanceState
					.getSerializable("provider");
			getCredentials();
		} else {
			boolean prefs = false;
			mConsumer = new CommonsHttpOAuthConsumer(Keys.TWITTER_CONSUMER_KEY,
					Keys.TWITTER_CONSUMER_SECRET, SignatureMethod.HMAC_SHA1);

			SharedPreferences settings = this.getSharedPreferences(PREFS, 0);
			if (settings.contains(TOKEN_STRING) && settings.contains(SECRET_STRING)) {
				String token = settings.getString(TOKEN_STRING, "");
				String secret = settings.getString(SECRET_STRING, "");
				mConsumer.setTokenWithSecret(token, secret);
				prefs = true;
			}
			mProvider = new DefaultOAuthProvider(mConsumer,
					TWITTER_REQUEST_TOKEN_URL, TWITTER_ACCESS_TOKEN_URL,
					TWITTER_AUTHORIZE_URL);
			if(prefs)
				getCredentials();
		}
		mButton.setOnClickListener(this);
		mCB.setOnClickListener(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle b) {
		b.putSerializable("provider", mProvider);
		b.putSerializable("consumer", mConsumer);
	}

	@Override
	protected void onNewIntent(Intent i) {

		Uri uri = i.getData();
		if (uri != null && CALLBACK_URI.getScheme().equals(uri.getScheme())) {
			try {
				String verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);
				mProvider.retrieveAccessToken(verifier);
				String token = mConsumer.getToken();
				String secret = mConsumer.getTokenSecret();
				this.saveAuthInformation(token, secret);
				getCredentials();
			} catch (OAuthMessageSignerException e) {
				e.printStackTrace();
			} catch (OAuthNotAuthorizedException e) {
				e.printStackTrace();
			} catch (OAuthExpectationFailedException e) {
				e.printStackTrace();
			} catch (OAuthCommunicationException e) {
				e.printStackTrace();
			}
		}
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
			return "";
		}
	}

	@Override
	public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
		String authUrl = null;

		if (arg1) {
			try {
				authUrl = mProvider.retrieveRequestToken(CALLBACK_URI
						.toString());
				Log.d(TAG, "AuthUrl: " + authUrl);
				Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
				this.startActivity(i);
			} catch (OAuthMessageSignerException e) {
				e.printStackTrace();
			} catch (OAuthNotAuthorizedException e) {
				e.printStackTrace();
			} catch (OAuthExpectationFailedException e) {
				e.printStackTrace();
			} catch (OAuthCommunicationException e) {
				e.printStackTrace();
			}
		} else {
			saveAuthInformation(null, null);
			getCredentials();
		}
	}

	public HttpParams getParams() {
		// Tweak further as needed for your app
		HttpParams params = new BasicHttpParams();
		// set this to false, or else you'll get an Expectation Failed: error
		HttpProtocolParams.setUseExpectContinue(params, false);
		return params;
	}


	public void getCredentials() {
		new GetCredentialsTask().execute();
	}

	private class GetCredentialsTask extends AsyncTask<Void, Void, JSONObject> {

		ProgressDialog authDialog;

		@Override
		protected void onPreExecute() {
			authDialog = ProgressDialog.show(BLOA.this, getText(R.string.auth_progress_title), getText(R.string.auth_progress_text), true);
		}
		
		@Override
		protected JSONObject doInBackground(Void... arg0) {
			JSONObject jso = null;
			DefaultHttpClient mClient = new DefaultHttpClient();
			try {
				HttpGet get = new HttpGet("http://twitter.com/account/verify_credentials.json");
				mConsumer.sign(get);
				String response = mClient.execute(get, new BasicResponseHandler());
				jso = new JSONObject(response);
				Log.d(TAG, "Credentials: " + jso.toString(2));
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
			} finally {
				mClient.getConnectionManager().shutdown();
			}
			return jso;
		}
		
		protected void onPostExecute(JSONObject jso) {
			authDialog.dismiss();
			if(jso != null) { // authorization succeeded, the json object contains the user information
				if(!mCB.isChecked())
					mCB.setChecked(true);
				mButton.setEnabled(true);
				mEditor.setEnabled(true);
				mUser.setText(getUserName(jso));
				mDisplay.setText(getLastTweet(jso));
			} else {
				if(mCB.isEnabled())
					mCB.setChecked(false);
				mButton.setEnabled(false);
				mEditor.setEnabled(false);
				mCB.setChecked(false);
				mDisplay.setText("");
			}
		}
	}
	
	private void saveAuthInformation(String token, String secret) {
		// null means to clear the old values
		SharedPreferences settings = BLOA.this.getSharedPreferences(PREFS, 0);
		SharedPreferences.Editor editor = settings.edit();
		if(token == null) {
			editor.remove(TOKEN_STRING);
			Log.d(TAG, "Clearing OAuth Token");
		}
		else {
			editor.putString(TOKEN_STRING, token);
			Log.d(TAG, "Saving OAuth Token: " + token);
		}
		if (secret == null) {
			editor.remove(SECRET_STRING);
			Log.d(TAG, "Clearing OAuth Secret");
		}
		else {
			editor.putString(SECRET_STRING, secret);
			Log.d(TAG, "Saving OAuth Secret: " + secret);
		}
		editor.commit();
		
	}
	
	private class PostTask extends AsyncTask<String, Void, JSONObject> {

		ProgressDialog postDialog;

		@Override
		protected void onPreExecute() {
			postDialog = ProgressDialog.show(BLOA.this, getText(R.string.tweet_progress_title), getText(R.string.tweet_progress_text), true);
		}
		
		@Override
		protected JSONObject doInBackground(String... params) {

			DefaultHttpClient mClient = new DefaultHttpClient();
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
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} finally {
				mClient.getConnectionManager().shutdown();
			}
			return jso;
		}
		
		// This is in the UI thread, so we can mess with the UI
		protected void onPostExecute(JSONObject jso) {
			postDialog.dismiss();
			if(jso != null) { // authorization succeeded, the json object contains the user information
				mEditor.setText("");
				mDisplay.setText(getCurrentTweet(jso));
			} else {
				mDisplay.setText(getText(R.string.tweet_error));
			}
		}
	}
	
	private String getCurrentTweet(JSONObject response) {
		return response.optString("text", getString(R.string.bad_value));
	}

	@Override
	public void onClick(View v) {
		if(mCB.equals(v)) {
			if(mCB.isChecked()) {
				try {
					String authUrl = mProvider.retrieveRequestToken(CALLBACK_URI.toString());
					Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
					this.startActivity(i);
				} catch (OAuthMessageSignerException e) {
					e.printStackTrace();
				} catch (OAuthNotAuthorizedException e) {
					e.printStackTrace();
				} catch (OAuthExpectationFailedException e) {
					e.printStackTrace();
				} catch (OAuthCommunicationException e) {
					e.printStackTrace();
				}
			} else {
				saveAuthInformation(null, null);
				mButton.setEnabled(false);
				mEditor.setEnabled(false);
				mCB.setChecked(false);
				mDisplay.setText("");
			}
		} else if(mButton.equals(v)) {
			String postString = mEditor.getText().toString();
			if (postString.length() == 0) {
				Toast.makeText(this, getText(R.string.tweet_empty),
						Toast.LENGTH_SHORT).show();
			} else {
				new PostTask().execute(postString);
			}
		}
	}
}