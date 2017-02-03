package com.pchmn.rxsocialauth.auth;


import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.google.android.gms.auth.api.credentials.Credential;
import com.pchmn.rxsocialauth.common.RxAccount;
import com.pchmn.rxsocialauth.common.RxStatus;

import rx.subjects.PublishSubject;

public class RxGoogleAuth {

    private static final String TAG = RxGoogleAuth.class.toString();
    // context
    private FragmentActivity mActivity;
    // google auth fragment
    private RxGoogleAuthFragment mRxGoogleAuthFragment;
    // subjects
    private PublishSubject<RxAccount> mAccountSubject;
    private PublishSubject<RxStatus> mStatusSubject;

    /**
     * Construct a RxGoogleAuth object from a RxGoogleAuth.Builder
     *
     * @param builder the builder
     */
    private RxGoogleAuth(RxGoogleAuth.Builder builder) {
        mActivity = builder.activity;
        mRxGoogleAuthFragment = getRxGoogleAuthFragment(builder);
    }

    /**
     * Builder inner class for RxGoogleAuth
     */
    public static class Builder {
        FragmentActivity activity;
        boolean requestEmail = false;
        boolean requestProfile = false;
        String clientId;
        boolean disableAutoSignIn = false;
        boolean enableSmartLock = false;

        public Builder(@NonNull FragmentActivity activity) {
            this.activity = activity;
        }

        /**
         * Request email
         *
         * @return the builder
         */
        public Builder requestEmail() {
            this.requestEmail = true;
            return this;
        }

        /**
         * Request profile
         *
         * @return the builder
         */
        public Builder requestProfile() {
            this.requestProfile = true;
            return this;
        }

        /**
         * Request an id token for authenticated users
         *
         * @param clientId the client id of the server that will verify the integrity of the token
         * @return the builder
         */
        public Builder requestIdToken(String clientId) {
            this.clientId = clientId;
            return this;
        }

        /**
         * Clear the account previously selected by the user, so the user will have to pick an account
         *
         * @return the builder
         */
        public Builder disableAutoSignIn() {
            this.disableAutoSignIn = true;
            return this;
        }

        /**
         * Enable or disable smart lock password
         *
         * @param enable true to enable, false to disable
         * @return the builder
         */
        public Builder enableSmartLock(boolean enable) {
            this.enableSmartLock = enable;
            return this;
        }

        /**
         * Build the RxGoogleAuth object
         *
         * @return the RxGoogleAuth object
         */
        public RxGoogleAuth build() {
            return new RxGoogleAuth(this);
        }
    }

    /**
     * Get instance of RxGoogleAuthFragment
     *
     * @return a RxGoogleAuthFragment
     */
    private RxGoogleAuthFragment getRxGoogleAuthFragment(Builder builder) {
        FragmentManager fragmentManager = mActivity.getSupportFragmentManager();

        // prevent fragment manager already executing transaction
        int stackCount = fragmentManager.getBackStackEntryCount();
        if( fragmentManager.getFragments() != null )
            fragmentManager = fragmentManager.getFragments().get( stackCount > 0 ? stackCount-1 : stackCount ).getChildFragmentManager();

        RxGoogleAuthFragment rxGoogleAuthFragment = (RxGoogleAuthFragment)
                mActivity.getSupportFragmentManager().findFragmentByTag(RxGoogleAuthFragment.TAG);

        if (rxGoogleAuthFragment == null) {
            rxGoogleAuthFragment = RxGoogleAuthFragment.newInstance(builder);
            fragmentManager
                    .beginTransaction()
                    .add(rxGoogleAuthFragment, RxGoogleAuthFragment.TAG)
                    .commit();
            fragmentManager.executePendingTransactions();
        }
        return rxGoogleAuthFragment;
    }

    /**
     * Google sign in by launching a GoogleAuthHiddenActivity
     *
     * @return a PublishSubject<RxAccount>
     */
    public PublishSubject<RxAccount> signIn() {
        mAccountSubject = PublishSubject.create();
        mRxGoogleAuthFragment.signIn(mAccountSubject, null);
        return mAccountSubject;
    }

    /**
     * Google silent sign in by launching a GoogleAuthHiddenActivity
     *
     * @return a PublishSubject<RxAccount>
     */
    public PublishSubject<RxAccount> silentSignIn(Credential credential) {
        mAccountSubject = PublishSubject.create();
        mRxGoogleAuthFragment.signIn(mAccountSubject, credential);
        return mAccountSubject;
    }

    /**
     * Google sign out by launching a GoogleAuthHiddenActivity
     *
     * @return a PublishSubject<RxStatus>
     */
    public PublishSubject<RxStatus> signOut() {
        mStatusSubject = PublishSubject.create();
        mRxGoogleAuthFragment.signOut(mStatusSubject);
        return mStatusSubject;
    }

    /**
     * Google sign out by launching a GoogleAuthHiddenActivity
     *
     * @return an PublishSubject<RxStatus>
     */
    public PublishSubject<RxStatus> revokeAccess() {
        mStatusSubject = PublishSubject.create();
        mRxGoogleAuthFragment.revokeAccess(mStatusSubject);
        return mStatusSubject;
    }
}
