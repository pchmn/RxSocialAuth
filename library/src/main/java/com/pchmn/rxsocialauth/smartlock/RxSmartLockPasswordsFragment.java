package com.pchmn.rxsocialauth.smartlock;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.pchmn.rxsocialauth.R;
import com.pchmn.rxsocialauth.auth.RxFacebookAuth;
import com.pchmn.rxsocialauth.auth.RxGoogleAuth;
import com.pchmn.rxsocialauth.common.RxAccount;
import com.pchmn.rxsocialauth.common.RxStatus;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

import static android.app.Activity.RESULT_OK;


public class RxSmartLockPasswordsFragment extends Fragment implements GoogleApiClient.ConnectionCallbacks {

    public static final String TAG = RxSmartLockPasswordsFragment.class.toString();
    // activity
    private FragmentActivity mActivity;
    // RC
    private static final int RC_READ = 0;
    private static final int RC_SAVE = 1;
    private static final int RC_SAVE_INTERN = 2;
    // options
    private CredentialRequest mCredentialRequest;
    private boolean mDisableAutoSignIn;
    // credential action
    private CredentialAction mCredentialAction;
    // credential
    private Credential mCredential;
    private SmartLockOptions mSmartLockOptions;
    // google api client
    private GoogleApiClient mCredentialsApiClient;
    // subjects
    private PublishSubject<Object> mAccountSubject;
    private PublishSubject<RxStatus> mStatusSubject;
    private PublishSubject<CredentialRequestResult> mRequestSubject;
    // current account
    private RxAccount mCurrentAccount;
    private PublishSubject<RxAccount> mAccountObservableIntern;


    public static RxSmartLockPasswordsFragment newInstance(RxSmartLockPasswords.Builder builder) {
        RxSmartLockPasswordsFragment rxSmartLockPasswordsFragment = new RxSmartLockPasswordsFragment();
        rxSmartLockPasswordsFragment.setCredentialRequest(builder.credentialRequestBuilder.build());
        rxSmartLockPasswordsFragment.setDisableAutoSignIn(builder.disableAutoSignIn);
        return rxSmartLockPasswordsFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();

        // smart lock password
        mCredentialsApiClient = new GoogleApiClient.Builder(mActivity)
                .addConnectionCallbacks(this)
                .addApi(Auth.CREDENTIALS_API)
                .build();
    }

    /**
     * Request credential
     */
    public void requestCredential(PublishSubject<CredentialRequestResult> requestSubject) {
        mCredentialAction = CredentialAction.REQUEST;
        mRequestSubject = requestSubject;
        mCredentialsApiClient.connect();
    }

    private void requestCredential() {
        // disable auto sign in
        if (mDisableAutoSignIn)
            Auth.CredentialsApi.disableAutoSignIn(mCredentialsApiClient);

        Auth.CredentialsApi.request(mCredentialsApiClient, mCredentialRequest).setResultCallback(
                new ResultCallback<CredentialRequestResult>() {
                    @Override
                    public void onResult(@NonNull CredentialRequestResult credentialRequestResult) {
                        mCredentialsApiClient.disconnect();
                        mRequestSubject.onNext(credentialRequestResult);
                        mRequestSubject.onCompleted();
                    }
                });
    }

    /**
     * Request credential and auto sign in user
     */
    public void requestCredentialAndAutoSignIn(PublishSubject<Object> accountSubject) {
        mCredentialAction = CredentialAction.REQUEST_AND_AUTO_SIGN_IN;
        mAccountSubject = accountSubject;
        mCredentialsApiClient.connect();
    }

    private void requestCredentialAndAutoSignIn() {
        // disable auto sign in
        if(mDisableAutoSignIn)
            Auth.CredentialsApi.disableAutoSignIn(mCredentialsApiClient);

        Auth.CredentialsApi.request(mCredentialsApiClient, mCredentialRequest).setResultCallback(
                new ResultCallback<CredentialRequestResult>() {
                    @Override
                    public void onResult(@NonNull CredentialRequestResult credentialRequestResult) {
                        if(credentialRequestResult.getStatus().isSuccess())
                            onCredentialRetrieved(credentialRequestResult.getCredential());
                        else
                            resolveResult(credentialRequestResult.getStatus());
                    }
                });
    }

    private void onCredentialRetrieved(Credential credential) {
        String accountType = credential.getAccountType();
        // google account
        if (accountType != null && accountType.equals(IdentityProviders.GOOGLE)) {
            // build RxGoogleAuth object
            RxGoogleAuth.Builder builder = new RxGoogleAuth.Builder(mActivity);
            final GoogleOptions options = SmartLockHelper.getInstance(mActivity)
                    .getGoogleSmartLockOptions();

            if(options != null) {
                if(options.requestEmail)
                    builder.requestEmail();
                if(options.requestProfile)
                    builder.requestProfile();
                if(options.clientId != null)
                    builder.requestIdToken(options.clientId);
            }
            else {
                builder.requestEmail().requestProfile();
            }

            // silent sign in
            builder.build().silentSignIn(credential)
                    .flatMap(new Func1<RxAccount, Observable<RxAccount>>() {
                        @Override
                        public Observable<RxAccount> call(RxAccount account) {
                            mCurrentAccount = account;
                            Credential newCredential = new Credential.Builder(account.getEmail())
                                    .setAccountType(IdentityProviders.GOOGLE)
                                    .setName(account.getDisplayName())
                                    .setProfilePictureUri(account.getPhotoUri())
                                    .build();
                            // save credentials
                            return saveCredentialWithOptions(newCredential, options);
                        }
                    })
                    .subscribe(new Action1<RxAccount>() {
                        @Override
                        public void call(RxAccount account) {
                            mCredentialsApiClient.disconnect();
                            mAccountSubject.onNext(account);
                            mAccountSubject.onCompleted();

                        }

                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            mCredentialsApiClient.disconnect();
                            mAccountSubject.onError(throwable);
                        }
                    });
        }
        // facebook account
        else if(accountType != null && accountType.equals(IdentityProviders.FACEBOOK)) {
            // build rxFacebookAuth object
            RxFacebookAuth.Builder builder = new RxFacebookAuth.Builder(mActivity);
            final FacebookOptions options = SmartLockHelper.getInstance(mActivity)
                    .getFacebookSmartLockOptions();

            if(options != null) {
                if(options.requestEmail)
                    builder.requestEmail();
                if(options.requestProfile)
                    builder.requestProfile();
                builder.requestPhotoSize(options.photoWidth, options.photoHeight);
            }
            else {
                builder.requestEmail().requestProfile();
            }

            // sign in
            builder.build().signIn()
                    .flatMap(new Func1<RxAccount, Observable<RxAccount>>() {
                        @Override
                        public Observable<RxAccount> call(RxAccount account) {
                            mCurrentAccount = account;
                            Credential newCredential = new Credential.Builder(account.getEmail())
                                    .setAccountType(IdentityProviders.FACEBOOK)
                                    .setName(account.getDisplayName())
                                    .setProfilePictureUri(account.getPhotoUri())
                                    .build();
                            // save credentials
                            return saveCredentialWithOptions(newCredential, options);
                        }
                    })
                    .subscribe(new Action1<RxAccount>() {
                        @Override
                        public void call(RxAccount account) {
                            mCredentialsApiClient.disconnect();
                            mAccountSubject.onNext(account);
                            mAccountSubject.onCompleted();

                        }

                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            mCredentialsApiClient.disconnect();
                            mAccountSubject.onError(throwable);
                        }
                    });
        }
        // login password account or other provider
        else {
            mCredentialsApiClient.disconnect();
            mAccountSubject.onNext(credential);
            mAccountSubject.onCompleted();
        }
    }

    private void resolveResult(Status status) {
        if (status.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED) {
            try {
                //status.startResolutionForResult(mActivity, RC_READ);
                startIntentSenderForResult(status.getResolution().getIntentSender(), RC_READ, null, 0, 0, 0, null);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
                mCredentialsApiClient.disconnect();
                mAccountSubject.onError(new Throwable(e.toString()));
            }
        }
        else {
            // The user must create an account or sign in manually.
            mCredentialsApiClient.disconnect();
            mAccountSubject.onError(new Throwable(getString(R.string.status_canceled_request_credential)));
        }
    }

    /**
     * Save credential in smart lock for passwords and save options as well
     *
     * @param credential the credential
     * @param options the options
     * @return an Observable
     */
    private PublishSubject<RxAccount> saveCredentialWithOptions(Credential credential,
                                                                final SmartLockOptions options) {
        mAccountObservableIntern = PublishSubject.create();

        Auth.CredentialsApi.save(mCredentialsApiClient, credential).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            // save options
                            SmartLockHelper.getInstance(mActivity)
                                    .saveSmartLockOptions(options);
                            // credential saved
                            mCredentialsApiClient.disconnect();
                            mAccountObservableIntern.onNext(mCurrentAccount);
                            mAccountObservableIntern.onCompleted();
                            // reset current account
                            mCurrentAccount = null;

                        }
                        else if(status.hasResolution()) {
                            // Try to resolve the save request. This will prompt the user if
                            // the credential is new.
                            try {
                                status.startResolutionForResult(mActivity, RC_SAVE_INTERN);
                            } catch (IntentSender.SendIntentException e) {
                                // Could not resolve the request
                                mCredentialsApiClient.disconnect();
                                mAccountObservableIntern.onError(new Throwable(e.toString()));
                                // reset current account
                                mCurrentAccount = null;
                            }
                        }
                        else {
                            // request has no resolution
                            mCredentialsApiClient.disconnect();
                            mAccountObservableIntern.onCompleted();
                            // reset current account
                            mCurrentAccount = null;
                        }
                    }
                });

        return mAccountObservableIntern;
    }

    /**
     * Save credential
     */
    public void saveCredential(PublishSubject<RxStatus> statusObject, Credential credential) {
        mCredentialAction = CredentialAction.SAVE;
        mStatusSubject = statusObject;
        mCredential = credential;
        mCredentialsApiClient.connect();
    }

    private void saveCredential() {
        Auth.CredentialsApi.save(mCredentialsApiClient, mCredential).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            // clear smart lock options
                            SmartLockHelper.getInstance(mActivity).resetSmartLockOptions();
                            // credential saved
                            mCredentialsApiClient.disconnect();
                            mStatusSubject.onNext(new RxStatus(status));
                            mStatusSubject.onCompleted();
                        }
                        else if(status.hasResolution()) {
                            // Try to resolve the save request. This will prompt the user if
                            // the credential is new.
                            try {
                                status.startResolutionForResult(mActivity, RC_SAVE);
                            } catch (IntentSender.SendIntentException e) {
                                // Could not resolve the request
                                mCredentialsApiClient.disconnect();
                                mStatusSubject.onError(new Throwable(e.toString()));
                            }
                        }
                        else {
                            // request has no resolution
                            mCredentialsApiClient.disconnect();
                            mStatusSubject.onCompleted();
                        }
                    }
                });
    }

    public void saveCredentialWithOptions(PublishSubject<RxStatus> statusObject,
                                          Credential credential,
                                          SmartLockOptions smartLockOptions) {

        mCredentialAction = CredentialAction.SAVE_WITH_OPTIONS;
        mStatusSubject = statusObject;
        mCredential = credential;
        mSmartLockOptions = smartLockOptions;
        mCredentialsApiClient.connect();
    }

    private void saveCredentialWithOptions() {
        Auth.CredentialsApi.save(mCredentialsApiClient, mCredential).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            // save options
                            SmartLockHelper.getInstance(mActivity)
                                    .saveSmartLockOptions(mSmartLockOptions);
                            // credential saved
                            mCredentialsApiClient.disconnect();
                            mStatusSubject.onNext(new RxStatus(status));
                            mStatusSubject.onCompleted();
                        }
                        else if(status.hasResolution()) {
                            // Try to resolve the save request. This will prompt the user if
                            // the credential is new.
                            try {
                                status.startResolutionForResult(mActivity, RC_SAVE);
                            } catch (IntentSender.SendIntentException e) {
                                // Could not resolve the request
                                mCredentialsApiClient.disconnect();
                                mStatusSubject.onError(new Throwable(e.toString()));
                            }
                        }
                        else {
                            // request has no resolution
                            mCredentialsApiClient.disconnect();
                            mStatusSubject.onCompleted();
                        }
                    }
                });
    }

    /**
     * Delete credential
     */
    public void deleteCredential(PublishSubject<RxStatus> statusObject, Credential credential) {
        mCredentialAction = CredentialAction.DELETE;
        mStatusSubject = statusObject;
        mCredential = credential;
        mCredentialsApiClient.connect();
    }

    private void deleteCredential() {
        Auth.CredentialsApi.delete(mCredentialsApiClient, mCredential).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                mCredentialsApiClient.disconnect();
                mStatusSubject.onNext(new RxStatus(status));
                mStatusSubject.onCompleted();

            }
        });
    }

    /**
     * Disable auto sign in
     */
    public void disableAutoSignIn(PublishSubject<RxStatus> statusObject) {
        mCredentialAction = CredentialAction.DISABLE_AUTO_SIGN_IN;
        mStatusSubject = statusObject;
        mCredentialsApiClient.connect();
    }

    private void disableAutoSignIn() {
        Auth.CredentialsApi.disableAutoSignIn(mCredentialsApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                mCredentialsApiClient.disconnect();
                mStatusSubject.onNext(new RxStatus(status));
                mStatusSubject.onCompleted();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_READ) {
            if (resultCode == RESULT_OK) {
                Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                onCredentialRetrieved(credential);
            } else {
                // The user must create an account or sign in manually.
                mCredentialsApiClient.disconnect();
                mAccountSubject.onError(new Throwable(getString(R.string.status_canceled_request_credential)));
            }
        }
        else if (requestCode == RC_SAVE) {
            mCredentialsApiClient.disconnect();

            if (resultCode == RESULT_OK) {
                // credentials saved
                mStatusSubject.onNext(new RxStatus(
                        CommonStatusCodes.SUCCESS,
                        getString(R.string.status_success_credential_saved_message)
                ));
                mStatusSubject.onCompleted();
            } else {
                // cancel by user
                mStatusSubject.onNext(new RxStatus(
                        CommonStatusCodes.CANCELED,
                        getString(R.string.status_canceled_credential_saved_message)
                ));
                mStatusSubject.onCompleted();
            }
        }
        else if (requestCode == RC_SAVE_INTERN) {
            mCredentialsApiClient.disconnect();

            if (resultCode == RESULT_OK) {
                // credentials saved
                mAccountObservableIntern.onNext(mCurrentAccount);
                mAccountObservableIntern.onCompleted();
                // reset current account
                mCurrentAccount = null;
            } else {
                // cancel by user
                mAccountObservableIntern.onNext(mCurrentAccount);
                mAccountObservableIntern.onCompleted();
                // reset current account
                mCurrentAccount = null;
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        switch (mCredentialAction) {
            case REQUEST:
                requestCredential();
                break;
            case REQUEST_AND_AUTO_SIGN_IN:
                requestCredentialAndAutoSignIn();
                break;
            case DELETE:
                deleteCredential();
                break;
            case DISABLE_AUTO_SIGN_IN:
                disableAutoSignIn();
                break;
            case SAVE:
                saveCredential();
                break;
            case SAVE_WITH_OPTIONS:
                saveCredentialWithOptions();
                break;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    public void setCredentialRequest(CredentialRequest credentialRequest) {
        mCredentialRequest = credentialRequest;
    }

    public void setDisableAutoSignIn(boolean mDisableAutoSignIn) {
        this.mDisableAutoSignIn = mDisableAutoSignIn;
    }

    enum CredentialAction {
        REQUEST, REQUEST_AND_AUTO_SIGN_IN, SAVE, SAVE_WITH_OPTIONS, DELETE, DISABLE_AUTO_SIGN_IN
    }
}
