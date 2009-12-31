package com.eyebrowsoftware.bloa;

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
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class BrionLearnsOAuth extends Activity implements OnCheckedChangeListener, OnClickListener {
	public static final String TAG = "BrionLearnsOAuth";

	public static final String TWITTER_CONSUMER_KEY = "6uWmSQRhKlQi7xNtj0w";
	public static final String TWITTER_CONSUMER_SECRET = "Ilm3c0D7BV6GkTMpcPLMwFtMyeUYepa3hoIBqZUL9w";
	public static final String TWITTER_REQUEST_TOKEN_URL = "http://twitter.com/oauth/request_token";
	public static final String TWITTER_ACCESS_TOKEN_URL = "http://twitter.com/oauth/access_token";
	public static final String TWITTER_AUTHORIZE_URL = "http://twitter.com/oauth/authorize";
	
	private static final Uri CALLBACK_URI = Uri.parse("bloa-app://twitt");
	
	private static final String PREFS = "MyPrefsFile";
	
	private OAuthConsumer mConsumer;
	private OAuthProvider mProvider;
	
	private DefaultHttpClient mClient;
	
	private CheckBox mCB;
	private EditText mEditor;
	private Button mButton;
	private LinearLayout mEditGroup;
	private TextView mDisplay;
	
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
        mEditGroup = (LinearLayout) this.findViewById(R.id.edit_group);
        mEditGroup.setEnabled(false);
        mDisplay = (TextView) this.findViewById(R.id.last);
        
        if(savedInstanceState != null) {
        	mConsumer = (OAuthConsumer) savedInstanceState.getSerializable("consumer");
        	mProvider = (OAuthProvider) savedInstanceState.getSerializable("provider");
        	String token = mConsumer.getToken();
        	String secret = mConsumer.getTokenSecret();
        	if(token != null && secret != null) {
	 	    	Log.d(TAG, "From Saved!! Token: " + token + ", Secret: " + secret);
        		// mCB.setChecked(true);
        		// mEditGroup.setEnabled(true);
        	}
        } else {
	        mConsumer = new CommonsHttpOAuthConsumer(
	        		TWITTER_CONSUMER_KEY,
	        		TWITTER_CONSUMER_SECRET,
	        		SignatureMethod.HMAC_SHA1);
	 	    
	        SharedPreferences settings = this.getSharedPreferences(PREFS, 0);
	 	    if(settings.contains(TOKEN_STRING) 
	 	    		&& settings.contains(SECRET_STRING)) {
	 	    	String token = settings.getString(TOKEN_STRING, "");
	 	    	String secret = settings.getString(SECRET_STRING, "");
	 	    	Log.d(TAG, "From preferences!! Token: " + token + ", Secret: " + secret);
	 	    	mConsumer.setTokenWithSecret(token, secret);
        		mCB.setChecked(true);
        		mEditGroup.setEnabled(true);
	 	    }
			mProvider = new DefaultOAuthProvider(mConsumer,TWITTER_REQUEST_TOKEN_URL,TWITTER_ACCESS_TOKEN_URL,TWITTER_AUTHORIZE_URL);
        }
        mButton.setOnClickListener(this);
        mCB.setOnCheckedChangeListener(this);
        mClient = new DefaultHttpClient();
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
	 	    	Log.d(TAG, "New OAuth Token: " + token + ", New Secret: " + secret);
	 	    	
         	    SharedPreferences settings = this.getSharedPreferences(PREFS, 0);
        	    SharedPreferences.Editor editor = settings.edit();
        	    if(!settings.contains(TOKEN_STRING))
        	    		editor.putString(TOKEN_STRING, token);
        	    if(!settings.contains(SECRET_STRING))
        	    	editor.putString(SECRET_STRING, secret);
        	    editor.commit();

        	    mEditGroup.setEnabled(true);
        	    
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
    		
    	}
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
		mClient.getConnectionManager().shutdown();
    }
    
	@Override
	public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
 		String authUrl = null;
 		
 		if(arg1) {
 	 		try {
				authUrl = mProvider.retrieveRequestToken(CALLBACK_URI.toString());
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
    	    SharedPreferences settings = this.getSharedPreferences(PREFS, 0);
    	    SharedPreferences.Editor editor = settings.edit();
    	    editor.remove(TOKEN_STRING);
    	    editor.remove(SECRET_STRING);
    	    editor.commit();

    	    mEditGroup.setEnabled(false);
    	    mCB.setChecked(false);
    	    finish();
 		}
	}

    public HttpParams getParams() { 
        //Tweak further as needed for your app 
        HttpParams params = new BasicHttpParams(); 
        // set this to false, or else you'll get an Expectation Failed: error 
        HttpProtocolParams.setUseExpectContinue(params, false); 
        return params; 
    }
    
	@Override
	public void onClick(View v) {
		try {
			String response;
			HttpContext c = new BasicHttpContext();
			// First let's verify our credentials
			HttpGet get = new HttpGet("http://twitter.com/account/verify_credentials.json");

			// sign the request to authenticate
			mConsumer.sign(get);
			
	        response = mClient.execute(get, new BasicResponseHandler(), c);
	        JSONObject respJSO = new JSONObject(response);
	        Log.d(TAG, "Credentials: " + respJSO.toString(2));
			
			// create a request that requires authentication
	        HttpPost post = new HttpPost("http://twitter.com/statuses/update.json");
	        LinkedList<BasicNameValuePair> out = new LinkedList<BasicNameValuePair>();
	        
	        // 'status' here is the update value you collect from UI
	        out.add(new BasicNameValuePair("status", mEditor.getText().toString()));
	        HttpEntity e = new UrlEncodedFormEntity(out, HTTP.UTF_8);
			post.setEntity(e);
	        post.setParams(this.getParams());
	        
	        // sign the request to authenticate
			mConsumer.sign(post);
			
	        // send the request
			// HttpContext c = new BasicHttpContext();
	        response = mClient.execute(post, new BasicResponseHandler(), c);
	        mEditor.setText("");

	        JSONObject jso = new JSONObject(response);
	        Log.d(TAG, "Response: " + jso.toString(2));
	        String status = jso.optString("text", "Bad Value");
	        mDisplay.setText(status);
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
			
		}
	}
}