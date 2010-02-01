package com.example.bloa;

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
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class OAUTH extends Activity {
	private static final String TAG = "OAUTH";

	public static final String USER_TOKEN = "user_token";
	public static final String USER_SECRET = "user_secret";
	public static final String REQUEST_TOKEN = "request_token";
	public static final String REQUEST_SECRET = "request_secret";

	public static final String TWITTER_REQUEST_TOKEN_URL = "http://twitter.com/oauth/request_token";
	public static final String TWITTER_ACCESS_TOKEN_URL = "http://twitter.com/oauth/access_token";
	public static final String TWITTER_AUTHORIZE_URL = "http://twitter.com/oauth/authorize";

	private static final Uri CALLBACK_URI = Uri.parse("bloa-app://twitt");

	private OAuthConsumer mConsumer = null;
	private OAuthProvider mProvider = null;

	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
	
		// We don't need to worry about any saved states: we can reconstruct the state
		mConsumer = new CommonsHttpOAuthConsumer(
				Keys.TWITTER_CONSUMER_KEY, 
				Keys.TWITTER_CONSUMER_SECRET);
		
		mProvider = new DefaultOAuthProvider(
				TWITTER_REQUEST_TOKEN_URL, 
				TWITTER_ACCESS_TOKEN_URL,
				TWITTER_AUTHORIZE_URL);
		
		// It turns out this was the missing thing to making standard Activity launch mode work
		mProvider.setOAuth10a(true);

		// Now we see if we saved information from previous request and we're in the process of
		// coming back with an oauth result. We have to restore the saved request token and secret
		// so they will be available in onResume() when get the OAUTH response with the verification information
		SharedPreferences settings = this.getSharedPreferences(BLOAProvider.PREFS, Context.MODE_PRIVATE);
		String token = settings.getString(OAUTH.REQUEST_TOKEN, null);
		String secret = settings.getString(OAUTH.REQUEST_SECRET, null);
		if(!(token == null || secret == null)) {
			mConsumer.setTokenWithSecret(token, secret);
			saveRequestInformation(this, null, null);
		} else {
			try {
				String authUrl = mProvider.retrieveRequestToken(mConsumer, CALLBACK_URI.toString());
				saveRequestInformation(this, mConsumer.getToken(), mConsumer.getTokenSecret());
				Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
				i.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
				// i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET 
				//		| Intent.FLAG_ACTIVITY_NEW_TASK);
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
		}
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
				mProvider.retrieveAccessToken(mConsumer, verifier);

				String token = mConsumer.getToken();
				String secret = mConsumer.getTokenSecret();
				OAUTH.saveAuthInformation(this, token, secret);
				Intent i = new Intent(this, BLOA.class);
				i.putExtra(USER_TOKEN, token);
				i.putExtra(USER_SECRET, secret);
				startActivity(i);
				// this.setResult(Activity.RESULT_OK, i);
			} catch (OAuthMessageSignerException e) {
				this.setResult(Activity.RESULT_FIRST_USER);
				e.printStackTrace();
			} catch (OAuthNotAuthorizedException e) {
				this.setResult(Activity.RESULT_FIRST_USER + 1);
				e.printStackTrace();
			} catch (OAuthExpectationFailedException e) {
				this.setResult(Activity.RESULT_FIRST_USER + 2);
				e.printStackTrace();
			} catch (OAuthCommunicationException e) {
				this.setResult(Activity.RESULT_FIRST_USER + 3);
				e.printStackTrace();
			} finally {
				finish();
			}
		}
	}
	
	public static void saveRequestInformation(Context context, String token, String secret) {
		// null means to clear the old values
		SharedPreferences settings = 
			context.getSharedPreferences(BLOAProvider.PREFS, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		if(token == null) {
			editor.remove(OAUTH.REQUEST_TOKEN);
			Log.d(TAG, "Clearing Request Token");
		}
		else {
			editor.putString(OAUTH.REQUEST_TOKEN, token);
			Log.d(TAG, "Saving Request Token: " + token);
		}
		if (secret == null) {
			editor.remove(OAUTH.REQUEST_SECRET);
			Log.d(TAG, "Clearing Request Secret");
		}
		else {
			editor.putString(OAUTH.REQUEST_SECRET, secret);
			Log.d(TAG, "Saving Request Secret: " + secret);
		}
		editor.commit();
		
	}
	
	public static void saveAuthInformation(Context context, String token, String secret) {
		// null means to clear the old values
		SharedPreferences settings = 
			context.getSharedPreferences(BLOAProvider.PREFS, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		if(token == null) {
			editor.remove(OAUTH.USER_TOKEN);
			Log.d(TAG, "Clearing OAuth Token");
		}
		else {
			editor.putString(OAUTH.USER_TOKEN, token);
			Log.d(TAG, "Saving OAuth Token: " + token);
		}
		if (secret == null) {
			editor.remove(OAUTH.USER_SECRET);
			Log.d(TAG, "Clearing OAuth Secret");
		}
		else {
			editor.putString(OAUTH.USER_SECRET, secret);
			Log.d(TAG, "Saving OAuth Secret: " + secret);
		}
		editor.commit();
		
	}
	
}
