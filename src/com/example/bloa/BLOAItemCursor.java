package com.example.bloa;

import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.database.AbstractCursor;

public class BLOAItemCursor extends AbstractCursor {
	private static final String TAG = "BLOACursor";
	
	JSONObject mObject = null;
	String[] mProjection;
	HashMap<String, String> mProjectionMap;
	String mKey;
	
	public BLOAItemCursor(String[] projection, JSONObject jso, String key, 
						HashMap<String, String> projection_map) throws JSONException {
		mProjectionMap = projection_map;
		mProjection = projection;
		mKey = key;
		if(key != null) { // paged list type
	    	mObject = jso.getJSONObject(key);
		} else {
			mObject = jso;
		}
	}

	@Override
	public String[] getColumnNames() {
		return mProjection;
	}

	@Override
	public int getCount() {
		return 1;
	}

	@Override
	public double getDouble(int column) {
		try {
			String key = mProjectionMap.get(mProjection[column]); 
			return mObject.getDouble(key);
		} catch (JSONException e) {
			e.printStackTrace();
			return 0.0;
		}
	}

	@Override
	public float getFloat(int column) {
		try {
			String key = mProjectionMap.get(mProjection[column]);
			return (mObject.isNull(key)) ? 0 : Float.valueOf(mObject.getString(key));
		} catch (JSONException e) {
			e.printStackTrace();
			return 0;
		}
	}

	@Override
	public int getInt(int column) {
		try {
			String key = mProjectionMap.get(mProjection[column]);
			return mObject.getInt(key);
		} catch (JSONException e) {
			e.printStackTrace();
			return 0;
		}
	}

	@Override
	public long getLong(int column) {
		try {
			String key = mProjectionMap.get(mProjection[column]);
			return mObject.getLong(key);
		} catch (JSONException e) {
			e.printStackTrace();
			return 0;
		}
	}

	@Override
	public short getShort(int column) {
		try {
			String key = mProjectionMap.get(mProjection[column]);
			return (mObject.isNull(key)) ? 0 : Short.valueOf(mObject.getString(key));
		} catch (JSONException e) {
			e.printStackTrace();
			return 0;
		}
	}

	private static final String empty = new String();
	
	@Override
	public String getString(int column) {
		try {
			String key = mProjectionMap.get(mProjection[column]);
			return (mObject.isNull(key)) ? empty : mObject.getString(key);
		} catch (JSONException e) {
			e.printStackTrace();
			return empty;
		}
	}

	@Override
	public boolean isNull(int column) {
		String key = mProjectionMap.get(mProjection[column]);
		return mObject.isNull(key);
	}


}
