package com.eyebrowssoftware.bloa.syncadapter;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.eyebrowssoftware.bloa.R;

public class SyncPreferenceActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.sync_preferences);
    }

}
