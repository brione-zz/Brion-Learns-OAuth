package com.eyebrowssoftware.bloa.syncadapter;

import junit.framework.Assert;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.eyebrowssoftware.bloa.Constants;
import com.eyebrowssoftware.bloa.R;
import com.eyebrowssoftware.bloa.data.BloaProvider;

public class SyncPreferenceActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
    static final String TAG = "SyncPreferenceActivity";

    private ListPreference mLp;
    private Account mAccount;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.sync_preferences);
        Assert.assertNotNull(mLp = (ListPreference) this.findPreference("pref_syncInterval"));
        AccountManager am = AccountManager.get(this);
        Account[] accounts = am.getAccountsByType(Constants.ACCOUNT_TYPE);
        Assert.assertTrue(accounts.length == 1); // There can only be one: Twitter
        mAccount = accounts[0];
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        String value = sharedPreferences.getString(key, getString(R.string.pref_syncIntervalDefault));
        int index = mLp.findIndexOfValue(value);
        String name = getString(R.string.unknown);
        if (index != -1) {
            name = mLp.getEntries()[index].toString();
        }
        mLp.setSummary(name);
        long longValue = Long.valueOf(value);
        if (longValue < 0) { // Off
            ContentResolver.setSyncAutomatically(mAccount, BloaProvider.AUTHORITY, false);
        } else {
            ContentResolver.setSyncAutomatically(mAccount, BloaProvider.AUTHORITY, true);
            // TODO: Control the periodic sync with the currently-empty bundle
            Bundle extras = new Bundle();
            // TODO: Just tried to set the "Do not repeat" extra from the ContentResolver and it crashed
            ContentResolver.addPeriodicSync(mAccount, BloaProvider.AUTHORITY, extras, longValue);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        this.onSharedPreferenceChanged(PreferenceManager.getDefaultSharedPreferences(this), "pref_syncInterval");
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
}
