package com.example.bloa.test;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.test.ActivityInstrumentationTestCase2;

import com.eyebrowssoftware.bloa.App;
import com.eyebrowssoftware.bloa.KeysProvider;
import com.eyebrowssoftware.bloa.activities.BloaActivity;

public class BloaActivityInstrumentationTestCase2 extends ActivityInstrumentationTestCase2<BloaActivity> {
    // You'll have to create your own
    KeysProvider mKeysProvider = new MyUserKeyProvider();
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
        editor.putString(App.USER_TOKEN, mKeysProvider.getKey1());
        editor.putString(App.USER_SECRET, mKeysProvider.getKey2());
        editor.commit();
        assertNotNull(this.getActivity());
    }
}
