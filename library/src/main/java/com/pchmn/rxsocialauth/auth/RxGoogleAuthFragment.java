package com.pchmn.rxsocialauth.auth;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

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
import com.pchmn.rxsocialauth.common.RxAccount;
import com.pchmn.rxsocialauth.common.RxStatus;
import com.pchmn.rxsocialauth.smartlock.GoogleOptions;
import com.pchmn.rxsocialauth.smartlock.RxSmartLockPasswords;

import rx.functions.Action1;
import rx.subjects.PublishSubject;

public class RxGoogleAuthFragment extends Fragment
        implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    public static final String TAG = RxGoogleAuthFragment.class.toString();
    // RC codes
    private static final int RC_SIGN_IN = 0;
    // activity
    private FragmentActivity mActivity;
    // auth action
    private AuthAction mAuthAction;
    private Credential mCredential;
    // options
    private boolean mRequestEmail;
    private boolean mRequestProfile;
    private String mClientId;
    private boolean mDisableAutoSignIn;
    private boolean mEnableSmartLock;
    // google api
    private GoogleApiClient mGoogleApiClient;
    // subjects
    private PublishSubject<RxAccount> mAccountSubject;
    private PublishSubject<RxStatus> mStatusSubject;


    public static RxGoogleAuthFragment newInstance(RxGoogleAuth.Builder builder) {
        RxGoogleAuthFragment rxGoogleAuthFragment = new RxGoogleAuthFragment();
        rxGoogleAuthFragment.setClientId(builder.clientId);
        rxGoogleAuthFragment.setDisableAutoSignIn(builder.disableAutoSignIn);
        rxGoogleAuthFragment.setEnableSmartLock(builder.enableSmartLock);
        rxGoogleAuthFragment.setRequestEmail(builder.requestEmail);
        rxGoogleAuthFragment.setRequestProfile(builder.requestProfile);
        return rxGoogleAuthFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
    }

    private GoogleSignInOptions buildGoogleSignInOptions(Credential credential) {
        GoogleSignInOptions.Builder builder =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN);

        if(mRequestEmail)
            builder.requestEmail();
        if(mRequestProfile)
            builder.requestProfile();
        if(mClientId != null)
            builder.requestIdToken(mClientId);
        if(credential != null)
            builder.setAccountName(credential.getId());

        return builder.build();
    }

    private GoogleApiClient buildGoogleApiClient(GoogleSignInOptions gso) {
        return new GoogleApiClient.Builder(mActivity)
                .addConnectionCallbacks(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    public void signIn(PublishSubject<RxAccount> accountSubject, Credential credential) {
        mAccountSubject = accountSubject;
        mCredential = credential;
        if(mCredential == null)
            mAuthAction = AuthAction.SIGN_IN;
        else
            mAuthAction = AuthAction.SILENT_SIGN_IN;
        mAuthAction = credential == null ? AuthAction.SIGN_IN: AuthAction.SILENT_SIGN_IN;
        mGoogleApiClient = buildGoogleApiClient(buildGoogleSignInOptions(credential));
        mGoogleApiClient.connect();
    }

    private void signIn() {
        if(mDisableAutoSignIn)
            Auth.GoogleSignInApi.signOut(mGoogleApiClient);

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        //startIntentSenderForResult(signInIntent.get, RC_SIGN_IN, null, 0, 0, 0, null);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

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
    public void signOut(PublishSubject<RxStatus> statusSubject) {
        mStatusSubject = statusSubject;
        mAuthAction = AuthAction.SIGN_OUT;
        if(mGoogleApiClient == null)
            mGoogleApiClient = buildGoogleApiClient(buildGoogleSignInOptions(null));
        mGoogleApiClient.connect();
    }

    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                mGoogleApiClient.disconnect();
                deleteCurrentUser();
                mStatusSubject.onNext(new RxStatus(status));
                mStatusSubject.onCompleted();
            }
        });
    }

    /**
     * Google revoke access
     */
    public void revokeAccess(PublishSubject<RxStatus> statusSubject) {
        mStatusSubject = statusSubject;
        mAuthAction = AuthAction.REVOKE_ACCESS;
        if(mGoogleApiClient == null)
            mGoogleApiClient = buildGoogleApiClient(buildGoogleSignInOptions(null));
        mGoogleApiClient.connect();
    }

    private void revokeAccess() {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                mGoogleApiClient.disconnect();
                deleteCurrentUser();
                mStatusSubject.onNext(new RxStatus(status));
                mStatusSubject.onCompleted();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

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
    public void handleSignInResult(final GoogleSignInResult result) {
        if (result.isSuccess()) {
            GoogleSignInAccount account = result.getSignInAccount();
            mGoogleApiClient.disconnect();
            verifySmartLockIsEnabled(new RxAccount(account));
        }
        else {
            // delete credential
            if(mCredential != null) {
                Auth.CredentialsApi.delete(mGoogleApiClient, mCredential).setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        mGoogleApiClient.disconnect();
                        mAccountSubject.onError(new Throwable(result.getStatus().toString()));
                    }
                });
            }
            else {
                mGoogleApiClient.disconnect();
                mAccountSubject.onError(new Throwable(result.getStatus().toString()));
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if(mAuthAction == AuthAction.SIGN_IN || mAuthAction == AuthAction.SILENT_SIGN_IN) {
            mGoogleApiClient.disconnect();
            mAccountSubject.onError(new Throwable(connectionResult.toString()));
        }
        else {
            mGoogleApiClient.disconnect();
            mStatusSubject.onError(new Throwable(connectionResult.toString()));
        }
    }

    private void verifySmartLockIsEnabled(final RxAccount account) {

        if(mEnableSmartLock) {
            Credential credential = new Credential.Builder(account.getEmail())
                    .setAccountType(IdentityProviders.GOOGLE)
                    .setName(account.getDisplayName())
                    .setProfilePictureUri(account.getPhotoUri())
                    .build();

            // save options
            GoogleOptions options = new GoogleOptions(mRequestEmail, mRequestProfile, mClientId);

            new RxSmartLockPasswords.Builder(mActivity).build().saveCredential(credential, options)
                    .subscribe(new Action1<RxStatus>() {
                        @Override
                        public void call(RxStatus status) {
                            if(status.isSuccess()) {
                                // save current user
                                saveCurrentUser(account);
                                // credential saved
                                mAccountSubject.onNext(account);
                                mAccountSubject.onCompleted();
                            }
                            else {
                                // fix Error occurred when trying to propagate error to Observer.onError
                                Throwable throwable = new Throwable(new Throwable(status.getMessage()));
                                mAccountSubject.onError(throwable);
                            }
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            mAccountSubject.onError(new Throwable(throwable));
                        }
                    });
        }
        else {
            // save current user
            saveCurrentUser(account);
            mAccountSubject.onNext(account);
            mAccountSubject.onCompleted();
        }
    }

    /**
     * Save the current user account
     *
     * @param account the current user account
     */
    public void saveCurrentUser(RxAccount account) {
        RxSocialAuth.getInstance().saveCurrentUser(account);
    }

    /**
     * Delete the current user
     */
    public void deleteCurrentUser() {
        RxSocialAuth.getInstance().deleteCurrentUser();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
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

    public void setRequestEmail(boolean mRequestEmail) {
        this.mRequestEmail = mRequestEmail;
    }

    public void setRequestProfile(boolean mRequestProfile) {
        this.mRequestProfile = mRequestProfile;
    }

    public void setClientId(String mClientId) {
        this.mClientId = mClientId;
    }

    public void setDisableAutoSignIn(boolean mDisableAutoSignIn) {
        this.mDisableAutoSignIn = mDisableAutoSignIn;
    }

    public void setEnableSmartLock(boolean mEnableSmartLock) {
        this.mEnableSmartLock = mEnableSmartLock;
    }

    enum AuthAction {
        SIGN_IN, SILENT_SIGN_IN, SIGN_OUT, REVOKE_ACCESS
    }
}
