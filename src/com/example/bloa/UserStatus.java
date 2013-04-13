package com.example.bloa;

import org.json.JSONException;
import org.json.JSONObject;

import android.text.format.Time;

public class UserStatus {

    JSONObject mStatus;
    JSONObject mUser;

 // Get stuff from the two types of Twitter JSONObject we deal with: credentials and status
    public static String getCurrentTweet(JSONObject status) {
        return status.optString("text", "Bad-value");
    }

    public UserStatus(JSONObject status) throws JSONException {

        mStatus = status;
        mUser = status.getJSONObject("user");
    }

    public long getId() {
        return mStatus.optLong("id", -1);
    }

    public String getUserName() {
        return mUser.optString("name", "Bad Value");
    }

    public String getText() {
        return getCurrentTweet(mStatus);
    }

    public String getCreatedAt() {
        @SuppressWarnings("unused")
        Time ret1 = new Time();
        return mStatus.optString("created_at", "Bad Value");
    }
}
