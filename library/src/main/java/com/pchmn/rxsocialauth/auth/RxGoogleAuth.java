package com.pchmn.rxsocialauth.auth;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.pchmn.rxsocialauth.smartlock.GoogleOptions;
import com.pchmn.rxsocialauth.smartlock.RxSmartLockPassword;

import rx.Observable;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

public class RxGoogleAuth implements IRxSocialAuth {

    private static final String TAG = RxGoogleAuth.class.toString();
    // context
    private Context mContext;
    // options
    private static boolean mRequestEmail;
    private static boolean mRequestProfile;
    private static boolean mDisableAutoSignIn;
    private static boolean mEnableSmartLock;
    private static String mClientId;
    // google auth action
    private static AuthAction mAuthAction;
    private static Credential mCredential;
    // publisher
    private static PublishSubject<RxAccount> mAccountObserver;
    private static PublishSubject<RxStatus> mStatusObserver;


    /**
     * Construct a RxGoogleAuth object from a RxGoogleAuth.Builder
     *
     * @param builder the builder
     */
    private RxGoogleAuth(RxGoogleAuth.Builder builder) {
        mContext = builder.context;
        mRequestEmail = builder.requestEmail;
        mRequestProfile = builder.requestProfile;
        mDisableAutoSignIn = builder.disableAutoSignIn;
        mEnableSmartLock = builder.enableSmartLock;
        mClientId = builder.idToken;
    }

    /**
     * Builder inner class for RxGoogleAuth
     */
    public static class Builder {
        private Context context;
        private boolean requestEmail = false;
        private boolean requestProfile = false;
        private String idToken;
        private boolean disableAutoSignIn = false;
        private boolean enableSmartLock = false;

        public Builder(Context context) {
            this.context = context;
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
            this.idToken = clientId;
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
     * Google sign in by launching a GoogleAuthHiddenActivity
     *
     * @return an Observable
     */
    @Override
    public PublishSubject<RxAccount> signIn() {
        mAccountObserver = PublishSubject.create();
        mAuthAction = AuthAction.SIGN_IN;
        mContext.startActivity(getIntent());
        return mAccountObserver;
    }

    /**
     * Google silent sign in by launching a GoogleAuthHiddenActivity
     *
     * @return an Observable
     */
    public PublishSubject<RxAccount> silentSignIn(Credential credential) {
        mAccountObserver = PublishSubject.create();
        mAuthAction = AuthAction.SILENT_SIGN_IN;
        mCredential = credential;
        mContext.startActivity(getIntent());
        return mAccountObserver;
    }

    /**
     * Google sign out by launching a GoogleAuthHiddenActivity
     *
     * @return an Observable
     */
    @Override
    public Observable<RxStatus> signOut() {
        mStatusObserver = PublishSubject.create();
        mAuthAction = AuthAction.SIGN_OUT;
        mContext.startActivity(getIntent());
        return mStatusObserver;
    }

    /**
     * Google sign out by launching a GoogleAuthHiddenActivity
     *
     * @return an Observable
     */
    public Observable<RxStatus> revokeAccess() {
        mStatusObserver = PublishSubject.create();
        mAuthAction = AuthAction.REVOKE_ACCESS;
        mContext.startActivity(getIntent());
        return mStatusObserver;
    }

    /**
     * Get intent for GoogleAuthHiddenActivity
     *
     * @return the intent
     */
    private Intent getIntent() {
        Intent intent = new Intent(mContext, GoogleAuthHiddenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /**
     * Inner activity that will do the work
     */
    public static class GoogleAuthHiddenActivity extends AppCompatActivity
            implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, IHiddenActivity {

        private static final int RC_SIGN_IN = 0;
        private GoogleApiClient mGoogleApiClient;

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            GoogleSignInOptions gso;

            // create GoogleSignInOptions object
            if(mAuthAction == AuthAction.SILENT_SIGN_IN)
                gso = buildGoogleSignInOptions(mCredential);
            else
                gso = buildGoogleSignInOptions(null);

            // create GoogleApiClient object
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this, this)
                    .addConnectionCallbacks(this)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();

            // action launched in OnConnected()
        }

        /**
         * Build a GoogleSignInOptions object
         *
         * @param credential the credential to use for silent sign in, can be null
         * @return
         */
        private GoogleSignInOptions buildGoogleSignInOptions(Credential credential) {
            GoogleSignInOptions.Builder gsoBuilder =
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN);
            if(mRequestEmail)
                gsoBuilder.requestEmail();
            if(mRequestProfile)
                gsoBuilder.requestProfile();
            if(mClientId != null)
                gsoBuilder.requestIdToken(mClientId);
            if(credential != null)
                gsoBuilder.setAccountName(credential.getId());

            return gsoBuilder.build();
        }

        /**
         * Regular google sign in
         */
        @Override
        public void signIn() {
            if(mDisableAutoSignIn)
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
            Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
            startActivityForResult(signInIntent, RC_SIGN_IN);
        }

        /**
         * Silent google sign in
         */
        private void silentSignIn() {
            OptionalPendingResult<GoogleSignInResult> pendingResult =
                    Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);

            if (pendingResult.isDone()) {
                handleSignInResult(pendingResult.get());
            } else {
                pendingResult.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                    @Override
                    public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                        handleSignInResult(googleSignInResult);
                    }
                });
            }
        }

        /**
         * Google sign out
         */
        @Override
        public void signOut() {
            Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    if(status.isSuccess())
                        deleteCurrentUser();
                    mStatusObserver.onNext(new RxStatus(status));
                    mStatusObserver.onCompleted();
                    finish();
                }
            });
        }

        /**
         * Google revoke access
         */
        private void revokeAccess() {
            Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    mStatusObserver.onNext(new RxStatus(status));
                    mStatusObserver.onCompleted();
                    finish();
                }
            });
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
            if (requestCode == RC_SIGN_IN) {
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                handleSignInResult(result);
            }
        }

        /**
         * Handle google sign in result
         *
         * @param result the GoogleSignInResult result
         */
        public void handleSignInResult(GoogleSignInResult result) {
            if (result.isSuccess()) {
                GoogleSignInAccount account = result.getSignInAccount();
                verifySmartLockIsEnabled(new RxAccount(account));
            }
            else {
                // fix Error occurred when trying to propagate error to Observer.onError
                Throwable throwable = new Throwable(new Throwable(result.getStatus().toString()));
                mAccountObserver.onError(throwable);
                finish();
            }
            finish();
        }

        /**
         * Verify if smart lock is enabled, save the account if it is
         *
         * @param account the social account to save
         */
        @Override
        public void verifySmartLockIsEnabled(final RxAccount account) {
            if(mEnableSmartLock) {
                Credential credential = new Credential.Builder(account.getEmail())
                        .setAccountType(IdentityProviders.GOOGLE)
                        .setName(account.getDisplayName())
                        .setProfilePictureUri(account.getPhotoUri())
                        .build();

                // save options
                GoogleOptions options = new GoogleOptions(mRequestEmail, mRequestProfile, mClientId);

                new RxSmartLockPassword.Builder(this).build().saveCredential(credential, options)
                        .subscribe(new Action1<RxStatus>() {
                            @Override
                            public void call(RxStatus status) {
                                if(status.isSuccess()) {
                                    // save current user
                                    saveCurrentUser(account);
                                    // credential saved
                                    mAccountObserver.onNext(account);
                                    mAccountObserver.onCompleted();
                                    finish();
                                }
                                else {
                                    Throwable throwable = new Throwable(new Throwable(status.toString()));
                                    mAccountObserver.onError(throwable);
                                    finish();
                                }
                            }
                        });
            }
            else {
                // save current user
                saveCurrentUser(account);
                mAccountObserver.onNext(account);
                mAccountObserver.onCompleted();
                finish();
            }
        }

        /**
         * Save the current user account
         *
         * @param account the current user account
         */
        @Override
        public void saveCurrentUser(RxAccount account) {
            RxSocialAuth.getInstance(this).saveCurrentUser(account);
        }

        /**
         * Delete the current user
         */
        @Override
        public void deleteCurrentUser() {
            RxSocialAuth.getInstance(this).deleteCurrentUser();
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            // launch action
            switch (mAuthAction) {
                case SIGN_IN:
                    signIn();
                    break;
                case SILENT_SIGN_IN:
                    silentSignIn();
                    break;
                case SIGN_OUT:
                    signOut();
                    break;
                case REVOKE_ACCESS:
                    revokeAccess();
                    break;
            }
        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            // fix Error occurred when trying to propagate error to Observer.onError
            Throwable throwable = new Throwable(new Throwable(connectionResult.getErrorMessage()));
            if(mAuthAction == AuthAction.SIGN_OUT || mAuthAction == AuthAction.REVOKE_ACCESS)
                mStatusObserver.onError(throwable);
            else
                mAccountObserver.onError(throwable);
            finish();
        }
    }
}
