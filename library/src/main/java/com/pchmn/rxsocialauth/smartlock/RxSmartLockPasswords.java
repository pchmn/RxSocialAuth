package com.pchmn.rxsocialauth.smartlock;


import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.pchmn.rxsocialauth.common.RxStatus;

import rx.subjects.PublishSubject;

public class RxSmartLockPasswords {

    private static final String TAG = RxSmartLockPasswords.class.toString();
    // activity
    private FragmentActivity mActivity;

    // google auth fragment
    private RxSmartLockPasswordsFragment mRxSmartLockPasswordsFragment;
    // subjects
    private PublishSubject<Object> mAccountSubject;
    private PublishSubject<RxStatus> mStatusSubject;
    private PublishSubject<CredentialRequestResult> mRequestSubject;

    /**
     * Construct a RxSmartLockPassword object from a RxSmartLockPassword.Builder
     *
     * @param builder the builder
     */
    private RxSmartLockPasswords(Builder builder) {
        mActivity = builder.activity;
        mRxSmartLockPasswordsFragment = getRxFacebookAuthFragment(builder);
    }

    /**
     * Builder inner class for RxFacebookAuth
     */
    public static class Builder {
        FragmentActivity activity;
        CredentialRequest.Builder credentialRequestBuilder;
        boolean disableAutoSignIn = false;

        public Builder(FragmentActivity activity) {
            this.activity = activity;
            credentialRequestBuilder = new CredentialRequest.Builder();
            // at least one provider (mandatory)
            credentialRequestBuilder.setAccountTypes(IdentityProviders.GOOGLE);
        }

        public Builder setPasswordLoginSupported(boolean supported) {
            credentialRequestBuilder.setPasswordLoginSupported(supported);
            return this;
        }

        public Builder setAccountTypes(String... accountTypes) {
            credentialRequestBuilder.setAccountTypes(accountTypes);
            return this;
        }

        public Builder disableAutoSignIn() {
            this.disableAutoSignIn = true;
            return this;
        }

        public RxSmartLockPasswords build() {
            return new RxSmartLockPasswords(this);
        }
    }

    /**
     * Get instance of RxSmartLockPasswordsFragment
     *
     * @return a RxSmartLockPasswordsFragment
     */
    private RxSmartLockPasswordsFragment getRxFacebookAuthFragment(Builder builder) {
        FragmentManager fragmentManager = mActivity.getSupportFragmentManager();

        // prevent fragment manager already executing transaction
        int stackCount = fragmentManager.getBackStackEntryCount();
        if( fragmentManager.getFragments() != null )
            fragmentManager = fragmentManager.getFragments().get( stackCount > 0 ? stackCount-1 : stackCount ).getChildFragmentManager();

        RxSmartLockPasswordsFragment rxSmartLockPasswordsFragment = (RxSmartLockPasswordsFragment)
                fragmentManager.findFragmentByTag(RxSmartLockPasswordsFragment.TAG);

        if (rxSmartLockPasswordsFragment == null) {
            rxSmartLockPasswordsFragment = RxSmartLockPasswordsFragment.newInstance(builder);
            fragmentManager
                    .beginTransaction()
                    .add(rxSmartLockPasswordsFragment, RxSmartLockPasswordsFragment.TAG)
                    .commit();
            fragmentManager.executePendingTransactions();
        }
        return rxSmartLockPasswordsFragment;
    }

    /**
     * Request credential
     *
     * @return a PublishSubject<CredentialRequestResult>
     */
    public PublishSubject<CredentialRequestResult> requestCredential() {
        mRequestSubject = PublishSubject.create();
        mRxSmartLockPasswordsFragment.requestCredential(mRequestSubject);
        return mRequestSubject;
    }

    /**
     * Request credential and auto sign in user
     *
     * @return a PublishSubject<Object>
     */
    public PublishSubject<Object> requestCredentialAndAutoSignIn() {
        mAccountSubject = PublishSubject.create();
        mRxSmartLockPasswordsFragment.requestCredentialAndAutoSignIn(mAccountSubject);
        return mAccountSubject;
    }

    /**
     * Save credential
     *
     * @param credential the credential
     * @return a PublishSubject<RxStatus>
     */
    public PublishSubject<RxStatus> saveCredential(Credential credential) {
        mStatusSubject = PublishSubject.create();
        mRxSmartLockPasswordsFragment.saveCredential(mStatusSubject, credential, null);
        return mStatusSubject;
    }

    /**
     * Save credential and save options
     *
     * @param credential the credential
     * @param smartLockOptions the options
     * @return a PublishSubject<RxStatus>
     */
    public PublishSubject<RxStatus> saveCredential(Credential credential, SmartLockOptions smartLockOptions) {
        mStatusSubject = PublishSubject.create();
        mRxSmartLockPasswordsFragment.saveCredential(mStatusSubject, credential, smartLockOptions);
        return mStatusSubject;
    }

    /**
     * Delete credential
     *
     * @return a PublishSubject<RxStatus>
     */
    public PublishSubject<RxStatus> deleteCredential(Credential credential) {
        mStatusSubject = PublishSubject.create();
        mRxSmartLockPasswordsFragment.deleteCredential(mStatusSubject, credential);
        return mStatusSubject;
    }

    /**
     * Delete credential
     *
     * @return a PublishSubject<RxStatus>
     */
    public PublishSubject<RxStatus> disableAutoSignIn() {
        mStatusSubject = PublishSubject.create();
        mRxSmartLockPasswordsFragment.disableAutoSignIn(mStatusSubject);
        return mStatusSubject;
    }
}
