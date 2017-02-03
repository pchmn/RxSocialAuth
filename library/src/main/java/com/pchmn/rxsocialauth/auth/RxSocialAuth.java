package com.pchmn.rxsocialauth.auth;


import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.pchmn.rxsocialauth.App;
import com.pchmn.rxsocialauth.R;
import com.pchmn.rxsocialauth.common.RxAccount;
import com.pchmn.rxsocialauth.common.RxStatus;
import com.pchmn.rxsocialauth.smartlock.RxSmartLockPasswords;

import java.io.IOException;

import rx.Observable;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

public class RxSocialAuth {

    private static final String TAG = RxSocialAuth.class.toString();
    // instance
    private static RxSocialAuth mInstance;
    // context
    private Context mContext;
    // shared preference
    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mEditor;
    private static final String CURRENT_USER_KEY = "current_user";
    // observer
    private PublishSubject<RxStatus> mStatusObserver;
    // auth state listener
    private RxAuthStateListener mRxAuthStateListener;

    /**
     * Construct a RxSocialAuth object from the application context
     */
    private RxSocialAuth() {
        mContext = App.getContext();
        mPrefs = mContext.getSharedPreferences(
                mContext.getString(R.string.preference_current_user), Context.MODE_PRIVATE);
        mEditor = mPrefs.edit();
    }

    /**
     * Get a unique instance of RxSocialAuth
     *
     * @return the instance of RxSocialAuth
     */
    public static synchronized RxSocialAuth getInstance() {
        if(mInstance == null)
            mInstance = new RxSocialAuth();
        return mInstance;
    }

    /**
     * Sign out from both Google and Facebook and disable auto sign in for Smart Lock Password.
     * Require a FragmentActivity
     *
     * @param activity the activity
     * @return a PublishSubject<RxStatus>
     */
    public PublishSubject<RxStatus> signOut(FragmentActivity activity) {
        mStatusObserver = PublishSubject.create();

        Observable<RxStatus> rxGoogleSignOut = new RxGoogleAuth.Builder(activity).build().signOut();
        Observable<RxStatus> rxFacebookSignOut = new RxFacebookAuth.Builder(activity).build().signOut();
        Observable<RxStatus> rxSmartLockDisableAutoSignin =
                new RxSmartLockPasswords.Builder(activity).build().disableAutoSignIn();

        Observable.merge(rxGoogleSignOut, rxFacebookSignOut, rxSmartLockDisableAutoSignin)
                .subscribe(new Action1<RxStatus>() {
                    @Override
                    public void call(RxStatus rxStatus) {
                        mStatusObserver.onNext(rxStatus);
                        mStatusObserver.onCompleted();
                    }
                });

        return mStatusObserver;
    }

    /**
     * Save current user account
     *
     * @param account the user account
     */
    public void saveCurrentUser(RxAccount account) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Uri.class, new UriAdapter())
                .create();
        String accountString = gson.toJson(account);
        mEditor.putString(CURRENT_USER_KEY, accountString).commit();
        // listener
        if(mRxAuthStateListener != null)
            mRxAuthStateListener.onAuthStateChanged(account);
    }

    /**
     * Get current user account
     *
     * @return the current user account
     */
    public RxAccount getCurrentUser() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Uri.class, new UriAdapter())
                .create();
        String account = mPrefs.getString(CURRENT_USER_KEY, null);
        return gson.fromJson(account, RxAccount.class);
    }

    /**
     * Delete the current user
     */
    public void deleteCurrentUser() {
        mEditor.clear().commit();
        // listener
        if(mRxAuthStateListener != null)
            mRxAuthStateListener.onAuthStateChanged(null);
    }

    /**
     * Uri adapter
     */
    public final class UriAdapter extends TypeAdapter<Uri> {
        @Override
        public void write(JsonWriter out, Uri uri) throws IOException {
            if(uri != null)
                out.value(uri.toString());
            else
                out.value("");
        }

        @Override
        public Uri read(JsonReader in) throws IOException {
            return Uri.parse(in.nextString());
        }
    }

    /**
     * Add an auth listener
     *
     * @param listener the listener to add
     */
    /*public void addRxAuthStateListener(RxAuthStateListener listener) {
        mRxAuthStateListener = listener;
        // first time
        mRxAuthStateListener.onAuthStateChanged(getCurrentUser());
    }*/

    /**
     * Remove the current auth state listener
     */
    /*public void removeRxAuthStateListener() {
        mRxAuthStateListener = null;
    }*/

    public interface RxAuthStateListener {
        void onAuthStateChanged(RxAccount newAccount);
    }
}
