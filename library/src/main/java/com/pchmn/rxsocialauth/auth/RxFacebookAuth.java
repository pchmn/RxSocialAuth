package com.pchmn.rxsocialauth.auth;


import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.pchmn.rxsocialauth.common.RxAccount;
import com.pchmn.rxsocialauth.common.RxStatus;

import rx.subjects.PublishSubject;

public class RxFacebookAuth {

    private static final String TAG = RxFacebookAuth.class.toString();
    // activity
    private FragmentActivity mActivity;

    // google auth fragment
    private RxFacebookAuthFragment mRxFacebookAuthFragment;
    // subjects
    private PublishSubject<RxAccount> mAccountSubject;
    private PublishSubject<RxStatus> mStatusSubject;

    /**
     * Construct a RxFacebookAuth object from a RxFacebookAuth.Builder
     *
     * @param builder the builder
     */
    private RxFacebookAuth(Builder builder) {
        mActivity = builder.activity;
        mRxFacebookAuthFragment = getRxFacebookAuthFragment(builder);
    }

    /**
     * Builder inner class for RxFacebookAuth
     */
    public static class Builder {
        FragmentActivity activity;
        boolean requestEmail = false;
        boolean requestProfile = false;
        int photoWidth = 50;
        int photoHeight = 50;
        boolean enableSmartLock = false;

        public Builder(FragmentActivity activity) {
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
         * Request a specific photo size, cause request profile
         *
         * @param width the width of the photo
         * @param height the height of the photo
         * @return the builder
         */
        public Builder requestPhotoSize(int width, int height) {
            this.requestProfile = true;
            this.photoWidth = width;
            this.photoHeight = height;
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
         * Build the RxFacebookAuth object
         *
         * @return the RxFacebookAuth object
         */
        public RxFacebookAuth build() {
            return new RxFacebookAuth(this);
        }
    }

    /**
     * Get instance of RxFacebookAuthFragment
     *
     * @return a RxFacebookAuthFragment
     */
    private RxFacebookAuthFragment getRxFacebookAuthFragment(Builder builder) {
        FragmentManager fragmentManager = mActivity.getSupportFragmentManager();

        // prevent fragment manager already executing transaction
        int stackCount = fragmentManager.getBackStackEntryCount();
        if( fragmentManager.getFragments() != null )
            fragmentManager = fragmentManager.getFragments().get( stackCount > 0 ? stackCount-1 : stackCount ).getChildFragmentManager();

        RxFacebookAuthFragment rxFacebookAuthFragment = (RxFacebookAuthFragment)
                fragmentManager.findFragmentByTag(RxFacebookAuthFragment.TAG);

        if (rxFacebookAuthFragment == null) {
            rxFacebookAuthFragment = RxFacebookAuthFragment.newInstance(builder);
            fragmentManager
                    .beginTransaction()
                    .add(rxFacebookAuthFragment, RxFacebookAuthFragment.TAG)
                    .commit();
            fragmentManager.executePendingTransactions();
        }
        return rxFacebookAuthFragment;
    }

    /**
     * Facebook sign in by launching a FacebookAuthHiddenActivity
     *
     * @return a PublishSubject<RxAccount>
     */
    public PublishSubject<RxAccount> signIn() {
        mAccountSubject = PublishSubject.create();
        mRxFacebookAuthFragment.signIn(mAccountSubject);
        return mAccountSubject;
    }

    /**
     * Facebook sign out by launching a FacebookAuthHiddenActivity
     *
     * @return a PublishSubject<RxStatus>
     */
    public PublishSubject<RxStatus> signOut() {
        mStatusSubject = PublishSubject.create();
        mRxFacebookAuthFragment.signOut(mStatusSubject);
        return mStatusSubject;
    }
}
