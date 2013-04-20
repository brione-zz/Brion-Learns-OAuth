package com.eyebrowssoftware.bloa.test;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;

import com.eyebrowssoftware.bloa.Constants;
import com.eyebrowssoftware.bloa.IKeysProvider;
import com.eyebrowssoftware.bloa.activities.BloaActivity;

public class BloaActivityInstrumentationTestCase2 extends ActivityInstrumentationTestCase2<BloaActivity> {
    // You'll have to create your own
    IKeysProvider mKeysProvider = new MyKeysProvider();
    SharedPreferences mSettings;

    public BloaActivityInstrumentationTestCase2() {
        super(BloaActivity.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        mSettings = PreferenceManager.getDefaultSharedPreferences(this.getInstrumentation().getTargetContext());
    }

    public void tearDown() throws Exception {
        mSettings = null;
        super.tearDown();
    }

    public void testPreconditions() {
        assertNotNull(this.getActivity());
    }

    public void testKeysSet() {
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString(Constants.USER_TOKEN, mKeysProvider.getKey1());
        editor.putString(Constants.USER_SECRET, mKeysProvider.getKey2());
        editor.commit();
        assertNotNull(this.getActivity());
    }
}
