package com.pchmn.rxsocialauth.smartlock;


import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.pchmn.rxsocialauth.R;

public class SmartLockHelper {

    private static final String TAG = SmartLockHelper.class.toString();
    // instance
    private static SmartLockHelper mInstance;
    // shared preference
    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mEditor;
    // keys
    private static final String GOOGLE_KEY = "google";
    private static final String FACEBOOK_KEY = "facebook";

    private SmartLockHelper(Context context) {
        mPrefs = context.getApplicationContext().getSharedPreferences(
                context.getString(R.string.preference_smartlock_helper), Context.MODE_PRIVATE);
        mEditor = mPrefs.edit();
    }

    public static synchronized SmartLockHelper getInstance(Context context) {
        if(mInstance == null)
            mInstance = new SmartLockHelper(context);
        return mInstance;
    }

    public void resetSmartLockOptions() {
        mEditor.clear().commit();
    }

    public void saveSmartLockOptions(SmartLockOptions smartLockOptions) {
        if(smartLockOptions instanceof GoogleOptions)
            saveGoogleSmartLockOptions(smartLockOptions);
        else if(smartLockOptions instanceof FacebookOptions)
            saveFacebookSmartLockOptions(smartLockOptions);
    }

    public void saveGoogleSmartLockOptions(SmartLockOptions smartLockOptions) {
        Gson gson = new Gson();
        String options = gson.toJson(smartLockOptions);
        mEditor.putString(GOOGLE_KEY, options).commit();
    }

    public void saveFacebookSmartLockOptions(SmartLockOptions smartLockOptions) {
        Gson gson = new Gson();
        String options = gson.toJson(smartLockOptions);
        mEditor.putString(FACEBOOK_KEY, options).commit();
    }

    public GoogleOptions getGoogleSmartLockOptions() {
        Gson gson = new Gson();
        String options = mPrefs.getString(GOOGLE_KEY, null);
        return gson.fromJson(options, GoogleOptions.class);
    }

    public FacebookOptions getFacebookSmartLockOptions() {
        Gson gson = new Gson();
        String options = mPrefs.getString(FACEBOOK_KEY, null);
        return gson.fromJson(options, FacebookOptions.class);
    }
}
