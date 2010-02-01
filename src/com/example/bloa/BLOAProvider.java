package com.example.bloa;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;

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
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class BLOAProvider extends ContentProvider {
	private static final String TAG = "BLOAProvider";

	public static final String PREFS = "MyPrefsFile";
	
	private static final String AUTHORITY = "com.example.bloa";
	public static final String BLOA_URI_STRING = "content://" + AUTHORITY;

	// We construct variations on these URIs based on the requests we receive
	// TODO: this will be a preference setting
	public static final String VERIFY_URL_STRING = "http://twitter.com/account/verify_credentials.json";
	public static final String PUBLIC_TIMELINE_URL_STRING = "http://twitter.com/statuses/public_timeline.json";
	public static final String USER_TIMELINE_URL_STRING = "http://twitter.com/statuses/user_timeline.json";
	public static final String HOME_TIMELINE_URL_STRING = "http://api.twitter.com/1/statuses/home_timeline.json";	
	public static final String FRIENDS_TIMELINE_URL_STRING = "http://api.twitter.com/1/statuses/friends_timeline.json";	
	public static final String STATUSES_URL_STRING = "http://twitter.com/statuses/update.json";	
	
	public static final Uri VERIFY_SERVER_URI = Uri.parse(VERIFY_URL_STRING);
	public static final Uri PUBLIC_TIMELINE_URI = Uri.parse(PUBLIC_TIMELINE_URL_STRING);
	public static final Uri USER_TIMELINE_URI = Uri.parse(USER_TIMELINE_URL_STRING);
	public static final Uri HOME_TIMELINE_URI = Uri.parse(HOME_TIMELINE_URL_STRING);	
	public static final Uri FRIENDS_TIMELINE_URI = Uri.parse(FRIENDS_TIMELINE_URL_STRING);	
	public static final Uri STATUSES_URI = Uri.parse(STATUSES_URL_STRING);	

    public static HashMap<String, String> sStatusesProjectionMap;
    public static HashMap<String, String> sUsersProjectionMap;
    public static HashMap<String, String> sAccountsProjectionMap;

    private static final UriMatcher sUriMatcher;
    
    public static final Uri CONTENT_URI = Uri.parse(BLOA_URI_STRING);
    
    DefaultHttpClient mClient;
    OAuthConsumer mConsumer;
    
	public BLOAProvider() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean onCreate() {
		mClient = new DefaultHttpClient();
		mConsumer = new CommonsHttpOAuthConsumer(
				Keys.TWITTER_CONSUMER_KEY, 
				Keys.TWITTER_CONSUMER_SECRET);
		
		return false;
	}
	
	@Override
	public void finalize() {
		mClient.getConnectionManager().shutdown();
	}

	@Override
	public String getType(Uri uri) {
		
        switch (sUriMatcher.match(uri)) {
        case PUBLIC_TIMELINE:
            return Statuses.Status.CONTENT_TYPE;
        case USER_TIMELINE:
        	return Statuses.Status.CONTENT_TYPE;
        case HOME_TIMELINE:
        	return Statuses.Status.CONTENT_TYPE;
        case USERS:
        	return Users.User.CONTENT_TYPE;
        case USER_ID:
        	return Users.User.CONTENT_ITEM_TYPE;
        case ACCOUNT_ID:
        	return Accounts.Account.CONTENT_ITEM_TYPE;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
	}

	public static final String AUTH_TOKEN = "auth_token";
	public static final String AUTH_SECRET = "auth_secret";
	public static final String SINCE_ID = "since_id";
	public static final String MAX_ID = "max_id";
	public static final String COUNT = "count";
	public static final String PAGE = "page";
	
	private JSONObject authenticatedQuery(Uri uri) {
		JSONObject jso = null;
    	HttpGet get = new HttpGet(uri.toString());
    	try {
			mConsumer.sign(get);
			String response = mClient.execute(get, new BasicResponseHandler());
			jso = new JSONObject(response);
			Log.d(TAG, "authenticatedQuery: " + jso.toString(2));
		} catch (OAuthMessageSignerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OAuthExpectationFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OAuthCommunicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jso;
	}
	
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		Uri sUri;
		String id_str;
		String since_id;
		String max_id;
		Uri.Builder builder;
		String auth_token = null;
		String auth_secret = null;
		JSONObject jso = null;
		Cursor c = null;
		
		// Technically, not all queries require authentication. But not requiring doesn't mean not allowed, so we're
		// going to require it for all queries
    	if((auth_token = uri.getQueryParameter("auth_token")) == null || 
    			((auth_secret = uri.getQueryParameter(AUTH_SECRET))) == null) {
    		throw new IllegalArgumentException("No OAUTH Credentials supplied");
    	}
    	mConsumer.setTokenWithSecret(auth_token, auth_secret);
		switch (sUriMatcher.match(uri)) {
		// Result is the Account's statuses
		case PUBLIC_TIMELINE:
        	jso = authenticatedQuery(PUBLIC_TIMELINE_URI);
			break;
		// Result is the Account's Home statuses
		case HOME_TIMELINE:
        	builder = HOME_TIMELINE_URI.buildUpon();
        	if((since_id = uri.getQueryParameter(SINCE_ID)) != null) {
        		builder.appendQueryParameter(SINCE_ID, since_id);
        	} else if((max_id = uri.getQueryParameter(MAX_ID)) != null) {
        		builder.appendQueryParameter(MAX_ID, max_id);
        	}
        	jso = authenticatedQuery(builder.build());
			break;
		case USER_TIMELINE:
        	jso = authenticatedQuery(USER_TIMELINE_URI);
			break;
		case OTHER_USER_TIMELINE:
        	jso = authenticatedQuery(USER_TIMELINE_URI.buildUpon().appendPath(uri.getLastPathSegment()).build());
			break;
		case USERS:
			break;
		case USER_ID:
			break;
		case ACCOUNT_ID:
			jso = authenticatedQuery(VERIFY_SERVER_URI);
			try {
				c = new BLOAItemCursor(projection, jso, null, sAccountsProjectionMap);
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
		}
		return c;
	}

	// These parameters are needed to talk to the messaging service
	public HttpParams getParams() {
		// Tweak further as needed for your app
		HttpParams params = new BasicHttpParams();
		// set this to false, or else you'll get an Expectation Failed: error
		HttpProtocolParams.setUseExpectContinue(params, false);
		return params;
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues cv) {

		Uri ret = null;
		
		switch (sUriMatcher.match(uri)) {
		// The effect here is to post a new tweet
		case STATUSES:
			JSONObject jso = null;
			try {
				HttpPost post = new HttpPost("http://twitter.com/statuses/update.json");
				LinkedList<BasicNameValuePair> out = new LinkedList<BasicNameValuePair>();
				
				out.add(new BasicNameValuePair("status", cv.getAsString("status")));
				post.setEntity(new UrlEncodedFormEntity(out, HTTP.UTF_8));
				post.setParams(getParams());
				// sign the request to authenticate
				mConsumer.sign(post);
				String response = mClient.execute(post, new BasicResponseHandler());
				jso = new JSONObject(response);
				long id = jso.getLong("id");
				ret = ContentUris.withAppendedId(uri, id);
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
			}
			break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
		}
		return ret;
	}

	@Override
	public int update(Uri arg0, ContentValues arg1, String arg2, String[] arg3) {
		return 0;
	}

	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		return 0;
	}
	
	private static final int PUBLIC_TIMELINE = 1; // no authentication
    private static final int HOME_TIMELINE = 2;
    private static final int OTHER_USER_TIMELINE = 4;
    private static final int USER_TIMELINE = 3;

    private static final int USERS = 10;
    private static final int USER_ID = 11;

    @SuppressWarnings("unused")
	private static final int ACCOUNTS = 20;
    private static final int ACCOUNT_ID = 21;
    
    private static final int STATUSES = 30;
    
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, "statuses", STATUSES);
        sUriMatcher.addURI(AUTHORITY, "public_timeline", PUBLIC_TIMELINE); // not user-specific, no auth
        sUriMatcher.addURI(AUTHORITY, "user_timeline", USER_TIMELINE);   // # is authenticated user's timeline
        sUriMatcher.addURI(AUTHORITY, "user_timeline/#", OTHER_USER_TIMELINE);   // # is user_id's timeline, auth reqd.
        sUriMatcher.addURI(AUTHORITY, "home_timeline", HOME_TIMELINE);   // authenticated user's home timeline

        sUriMatcher.addURI(AUTHORITY, "users", USERS);
        sUriMatcher.addURI(AUTHORITY, "users/#", USER_ID);
        
        sUriMatcher.addURI(AUTHORITY, "verify", ACCOUNT_ID);

        sStatusesProjectionMap = new HashMap<String, String>();
        sStatusesProjectionMap.put(Statuses.Status._ID, "id");
        sStatusesProjectionMap.put(Statuses.Status.TEXT, Statuses.Status.TEXT);
        sStatusesProjectionMap.put(Statuses.Status.SOURCE, Statuses.Status.SOURCE);
        sStatusesProjectionMap.put(Statuses.Status.USER, Statuses.Status.USER);
        
        sUsersProjectionMap = new HashMap<String, String>();
        sUsersProjectionMap.put(Users.User._ID, "id");
        sUsersProjectionMap.put(Users.User.NAME, Users.User.NAME);

        sAccountsProjectionMap = new HashMap<String, String>();
        sAccountsProjectionMap.put(Accounts.Account._ID, "id");
        sAccountsProjectionMap.put(Accounts.Account.NAME, Accounts.Account.NAME);
        sAccountsProjectionMap.put(Accounts.Account.STATUS, Accounts.Account.STATUS);
    }

}
