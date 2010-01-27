package com.example.bloa;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;

import junit.framework.Assert;
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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class BLOA extends Activity implements OnClickListener {
	public static final String TAG = "BLOA";

	public static final String TWITTER_REQUEST_TOKEN_URL = "http://twitter.com/oauth/request_token";
	public static final String TWITTER_ACCESS_TOKEN_URL = "http://twitter.com/oauth/access_token";
	public static final String TWITTER_AUTHORIZE_URL = "http://twitter.com/oauth/authorize";

	private static final Uri CALLBACK_URI = Uri.parse("bloa-app://twitt");

	private static final String PREFS = "MyPrefsFile";

	private OAuthConsumer mConsumer = null;
	private OAuthProvider mProvider = null;

	private CheckBox mCB;
	private EditText mEditor;
	private Button mButton;
	private TextView mDisplay;
	private TextView mUser;
	
	ProgressDialog postDialog = null;

	private static final String USER_TOKEN = "user_token";
	private static final String USER_SECRET = "user_secret";
	private static final String REQUEST_TOKEN = "request_token";
	private static final String REQUEST_SECRET = "request_secret";

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
		mButton.setOnClickListener(this);
		mCB.setOnClickListener(this);
		
		// We don't need to worry about any saved states: we can reconstruct the state
		mConsumer = new CommonsHttpOAuthConsumer(
				Keys.TWITTER_CONSUMER_KEY,
				Keys.TWITTER_CONSUMER_SECRET, 
				SignatureMethod.HMAC_SHA1);
		
		mProvider = new DefaultOAuthProvider(
				mConsumer,
				TWITTER_REQUEST_TOKEN_URL, 
				TWITTER_ACCESS_TOKEN_URL,
				TWITTER_AUTHORIZE_URL);
		
		// It turns out this was the missing thing to making standard Activity launch mode work
		mProvider.setOAuth10a(true);

		SharedPreferences settings = this.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
		String token;
		String secret;
		
		// We look for saved user keys first. If we find them, we'll test them in onResume()
		if(settings.contains(USER_TOKEN) && settings.contains(USER_SECRET)) {
			token = settings.getString(USER_TOKEN, null);
			secret = settings.getString(USER_SECRET, null);
			if(!(token == null || secret == null)) {
				mConsumer.setTokenWithSecret(token, secret);
			}
		} else 
			// Now we see if we saved information from previous request and we're in the process of
			// coming back with an oauth result. We have to restore the saved request token and secret
			// so they will be available in onResume() when get the OAUTH response with the verification information
			if(settings.contains(REQUEST_TOKEN) && settings.contains(REQUEST_SECRET)) {
			token = settings.getString(REQUEST_TOKEN, null);
			secret = settings.getString(REQUEST_SECRET, null);
			if(!(token == null || secret == null)) {
				mConsumer.setTokenWithSecret(token, secret);
			}
		}
		// You should do this (apparently) whenever you change the consumer in any way, like what we might have
		// done above. It doesn't hurt to get them reacquainted again. 
		mProvider.setConsumer(mConsumer);
	}

	@Override
	// Right now this will attempt to authenticate every time it is called.
	protected void onResume() {
		super.onResume();

		Uri uri = getIntent().getData();
		if (uri != null && CALLBACK_URI.getScheme().equals(uri.getScheme())) {
			try {
				String otoken = uri.getQueryParameter(OAuth.OAUTH_TOKEN);
				String verifier = uri.getQueryParameter(OAuth.OAUTH_VERIFIER);

				// We send out and save the request token, but the secret is not the same as the verifier
				// Apparently, the verifier is decoded to get the secret, which is then compared - crafty
				Assert.assertEquals(otoken, mConsumer.getToken());

				// This is the moment of truth - we could throw here
				mProvider.retrieveAccessToken(verifier);

				// Clear the saved request information, now that we have a blessed token/secret
				this.saveRequestInformation(null, null);
				this.saveAuthInformation(mConsumer.getToken(), mConsumer.getTokenSecret());
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
		new GetCredentialsTask().execute();
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

		@Override
		protected void onPreExecute() {
			authDialog = ProgressDialog.show(BLOA.this, 
					getText(R.string.auth_progress_title), 
					getText(R.string.auth_progress_text), 
					true,	// indeterminate duration
					false); // not cancel-able
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
		
		// This is in the UI thread, so we can mess with the UI
		protected void onPostExecute(JSONObject jso) {
			authDialog.dismiss();
			mCB.setChecked(jso != null);
			mButton.setEnabled(jso != null);
			mEditor.setEnabled(jso != null);
			mUser.setText(jso != null ? getUserName(jso) : getString(R.string.userhint));
			mDisplay.setText(jso != null ? getLastTweet(jso) : getString(R.string.userhint));
		}
	}
	
	//----------------------------
	// This task posts a message to your message queue on the service.
	private class PostTask extends AsyncTask<String, Void, JSONObject> {

		ProgressDialog postDialog;

		@Override
		protected void onPreExecute() {
			postDialog = ProgressDialog.show(BLOA.this, 
					getText(R.string.tweet_progress_title), 
					getText(R.string.tweet_progress_text), 
					true,	// indeterminate duration
					false); // not cancel-able
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
	
	@Override
	public void onClick(View v) {
		if(mCB.equals(v)) {
			if(mCB.isChecked()) {
				try {
					String authUrl = mProvider.retrieveRequestToken(CALLBACK_URI.toString());
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ObjectOutputStream oos = new ObjectOutputStream(baos);
					oos.writeObject(mProvider);
					oos.close();
					baos.toString();
					Log.d(TAG, "onClick() - AuthUrl: " + authUrl);
					Log.d(TAG, "onClick() - Request: " + mConsumer.getToken());
					Log.d(TAG, "onClick() - Secret: " + mConsumer.getTokenSecret());
					saveRequestInformation(mConsumer.getToken(), mConsumer.getTokenSecret());
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
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				saveAuthInformation(null, null);
				mButton.setEnabled(false);
				mEditor.setEnabled(false);
				mCB.setChecked(false);
				mDisplay.setText("");
			}
			mCB.setChecked(false); // the oauth callback will set it to the proper state
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

	private void saveRequestInformation(String token, String secret) {
		// null means to clear the old values
		SharedPreferences settings = BLOA.this.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		if(token == null) {
			editor.remove(REQUEST_TOKEN);
			Log.d(TAG, "Clearing Request Token");
		}
		else {
			editor.putString(REQUEST_TOKEN, token);
			Log.d(TAG, "Saving Request Token: " + token);
		}
		if (secret == null) {
			editor.remove(REQUEST_SECRET);
			Log.d(TAG, "Clearing Request Secret");
		}
		else {
			editor.putString(REQUEST_SECRET, secret);
			Log.d(TAG, "Saving Request Secret: " + secret);
		}
		editor.commit();
		
	}
	
	private void saveAuthInformation(String token, String secret) {
		// null means to clear the old values
		SharedPreferences settings = BLOA.this.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		if(token == null) {
			editor.remove(USER_TOKEN);
			Log.d(TAG, "Clearing OAuth Token");
		}
		else {
			editor.putString(USER_TOKEN, token);
			Log.d(TAG, "Saving OAuth Token: " + token);
		}
		if (secret == null) {
			editor.remove(USER_SECRET);
			Log.d(TAG, "Clearing OAuth Secret");
		}
		else {
			editor.putString(USER_SECRET, secret);
			Log.d(TAG, "Saving OAuth Secret: " + secret);
		}
		editor.commit();
		
	}
	
}