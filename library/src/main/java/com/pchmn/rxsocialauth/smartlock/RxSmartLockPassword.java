package com.pchmn.rxsocialauth.smartlock;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.pchmn.rxsocialauth.R;
import com.pchmn.rxsocialauth.auth.RxAccount;
import com.pchmn.rxsocialauth.auth.RxFacebookAuth;
import com.pchmn.rxsocialauth.auth.RxGoogleAuth;
import com.pchmn.rxsocialauth.auth.RxStatus;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

import static com.pchmn.rxsocialauth.smartlock.RxSmartLockPassword.CredentialAction.DELETE;
import static com.pchmn.rxsocialauth.smartlock.RxSmartLockPassword.CredentialAction.DISABLE_AUTO_SIGN_IN;
import static com.pchmn.rxsocialauth.smartlock.RxSmartLockPassword.CredentialAction.REQUEST;
import static com.pchmn.rxsocialauth.smartlock.RxSmartLockPassword.CredentialAction.REQUEST_AND_AUTO_SIGN_IN;
import static com.pchmn.rxsocialauth.smartlock.RxSmartLockPassword.CredentialAction.SAVE;
import static com.pchmn.rxsocialauth.smartlock.RxSmartLockPassword.CredentialAction.SAVE_WITH_OPTIONS;

public class RxSmartLockPassword {

    private static final String TAG = RxSmartLockPassword.class.toString();
    // context
    private Context mContext;
    private static CredentialRequest mCredentialRequest;
    private static Credential mCredential;
    private static boolean mDisableAutoSignIn;
    private static SmartLockOptions mSmartLockOptions;
    // credential action
    private static CredentialAction mCredentialAction;
    // publisher
    private static PublishSubject<Object> mAccountObservable;
    private static PublishSubject<CredentialRequestResult> mRequestObservable;
    private static PublishSubject<RxStatus> mStatusObservable;


    /**
     * Construct a RxSmartLockPassword object from a RxSmartLockPassword.Builder
     *
     * @param builder the builder
     */
    private RxSmartLockPassword(Builder builder) {
        mContext = builder.context;
        mCredentialRequest = builder.credentialRequestBuilder.build();
        mDisableAutoSignIn = builder.disableAutoSignIn;
    }

    /**
     * Builder inner class for RxFacebookAuth
     */
    public static class Builder {
        private Context context;
        private CredentialRequest.Builder credentialRequestBuilder;
        private boolean disableAutoSignIn = false;

        public Builder(Context context) {
            this.context = context;
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

        public RxSmartLockPassword build() {
            return new RxSmartLockPassword(this);
        }
    }

    /**
     * Request credential
     *
     * @return an Observable
     */
    public PublishSubject<CredentialRequestResult> requestCredential() {
        mRequestObservable = PublishSubject.create();
        mCredentialAction = REQUEST;
        mContext.startActivity(getIntent());
        return mRequestObservable;
    }

    /**
     * Request credential and auto sign in user
     *
     * @return an Observable
     */
    public PublishSubject<Object> requestCredentialAndAutoSignIn() {
        mAccountObservable = PublishSubject.create();
        mCredentialAction = REQUEST_AND_AUTO_SIGN_IN;
        mContext.startActivity(getIntent());
        return mAccountObservable;
    }

    /**
     * Save credential
     *
     * @param credential the credential
     * @return an Observable
     */
    public PublishSubject<RxStatus> saveCredential(Credential credential) {
        mStatusObservable = PublishSubject.create();
        mCredential = credential;
        mCredentialAction = SAVE;
        mContext.startActivity(getIntent());
        return mStatusObservable;
    }

    /**
     * Save credential and save options
     *
     * @param credential the credential
     * @param smartLockOptions the options
     * @return an Observable
     */
    public PublishSubject<RxStatus> saveCredential(Credential credential, SmartLockOptions smartLockOptions) {
        mStatusObservable = PublishSubject.create();
        mCredential = credential;
        mSmartLockOptions = smartLockOptions;
        mCredentialAction = SAVE_WITH_OPTIONS;
        mContext.startActivity(getIntent());
        return mStatusObservable;
    }

    /**
     * Delete credential
     *
     * @return an Observable
     */
    public PublishSubject<RxStatus> deleteCredential(Credential credential) {
        mStatusObservable = PublishSubject.create();
        mCredential = credential;
        mCredentialAction = DELETE;
        mContext.startActivity(getIntent());
        return mStatusObservable;
    }

    /**
     * Delete credential
     *
     * @return an Observable
     */
    public PublishSubject<RxStatus> disableAutoSignIn() {
        mStatusObservable = PublishSubject.create();
        mCredentialAction = DISABLE_AUTO_SIGN_IN;
        mContext.startActivity(getIntent());
        return mStatusObservable;
    }

    /**
     * Get intent for SmartLockHiddenActivity
     *
     * @return the intent
     */
    private Intent getIntent() {
        Intent intent = new Intent(mContext, SmartLockHiddenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /**
     * Inner activity that will do the work
     */
    public static class SmartLockHiddenActivity extends AppCompatActivity
            implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        private GoogleApiClient mCredentialsApiClient;
        private static final int RC_READ = 0;
        private static final int RC_SAVE = 1;
        private static final int RC_SAVE_INTERN = 2;
        private PublishSubject<RxAccount> mAccountObservableIntern;
        private RxAccount mCurrentAccount;

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_hidden_loading);

            // smart lock password
            mCredentialsApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .enableAutoManage(this, this)
                    .addApi(Auth.CREDENTIALS_API)
                    .build();

            // action launched in OnConnected()
        }

        /**
         * Request credential
         */
        private void requestCredential() {
            // disable auto sign in
            if(mDisableAutoSignIn)
                Auth.CredentialsApi.disableAutoSignIn(mCredentialsApiClient);

            Auth.CredentialsApi.request(mCredentialsApiClient, mCredentialRequest).setResultCallback(
                    new ResultCallback<CredentialRequestResult>() {
                        @Override
                        public void onResult(@NonNull CredentialRequestResult credentialRequestResult) {
                            mRequestObservable.onNext(credentialRequestResult);
                            mRequestObservable.onCompleted();
                            finish();
                        }
                    });
        }

        /**
         * Request credential and auto sign in user
         */
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
            // login password account
            if (accountType == null) {
                mAccountObservable.onNext(credential);
                mAccountObservable.onCompleted();
                finish();
            }
            // google account
            else if (accountType.equals(IdentityProviders.GOOGLE)) {
                // build RxGoogleAuth object
                RxGoogleAuth.Builder builder = new RxGoogleAuth.Builder(this);
                final GoogleOptions options = SmartLockHelper.getInstance(this)
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
                /*builder.build().silentSignIn(credential)
                        .subscribe(new Action1<RxAccount>() {
                            @Override
                            public void call(RxAccount rxAccount) {
                                mAccountObservable.onNext(rxAccount);
                                mAccountObservable.onCompleted();
                                finish();
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                mAccountObservable.onError(throwable);
                                finish();
                            }
                        });*/

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
                                mAccountObservable.onNext(account);
                                mAccountObservable.onCompleted();
                                finish();
                            }

                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                mAccountObservable.onError(throwable);
                                finish();
                            }
                        });
            }
            // facebook account
            else if(accountType.equals(IdentityProviders.FACEBOOK)) {
                // build rxFacebookAuth object
                RxFacebookAuth.Builder builder = new RxFacebookAuth.Builder(this);
                final FacebookOptions options = SmartLockHelper.getInstance(this)
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
                /*builder.build().signIn()
                        .subscribe(new Action1<RxAccount>() {
                            @Override
                            public void call(RxAccount rxAccount) {
                                mAccountObservable.onNext(rxAccount);
                                mAccountObservable.onCompleted();
                                finish();
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                mAccountObservable.onError(throwable);
                                finish();
                            }
                        });*/
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
                                mAccountObservable.onNext(account);
                                mAccountObservable.onCompleted();
                                finish();
                            }

                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                mAccountObservable.onError(throwable);
                                finish();
                            }
                        });
            }
        }

        private void resolveResult(Status status) {
            if (status.getStatusCode() == CommonStatusCodes.RESOLUTION_REQUIRED) {
                try {
                    status.startResolutionForResult(this, RC_READ);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                    mAccountObservable.onError(new Throwable(new Throwable(e.getMessage())));
                    finish();
                }
            }
            else {
                // The user must create an account or sign in manually.
                mAccountObservable.onError(new Throwable(getString(R.string.status_canceled_request_credential)));
                finish();
            }
        }

        /**
         * Save credential
         */
        private void saveCredential() {
            Auth.CredentialsApi.save(mCredentialsApiClient, mCredential).setResultCallback(
                    new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    if (status.isSuccess()) {
                        // clear smart lock options
                        SmartLockHelper.getInstance(SmartLockHiddenActivity.this).resetSmartLockOptions();
                        // credential saved
                        mStatusObservable.onNext(new RxStatus(status));
                        mStatusObservable.onCompleted();
                        finish();
                    }
                    else if(status.hasResolution()) {
                        // Try to resolve the save request. This will prompt the user if
                        // the credential is new.
                        try {
                            status.startResolutionForResult(SmartLockHiddenActivity.this, RC_SAVE);
                        } catch (IntentSender.SendIntentException e) {
                            // Could not resolve the request
                            Throwable throwable = new Throwable(new Throwable(e.getMessage()));
                            mStatusObservable.onError(throwable);
                            finish();
                        }
                    }
                    else {
                        // request has no resolution
                        mStatusObservable.onCompleted();
                        finish();
                    }
                }
            });
        }

        /**
         * Save credential and save options
         */
        private void saveCredentialWithOptions() {
            Auth.CredentialsApi.save(mCredentialsApiClient, mCredential).setResultCallback(
                    new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if (status.isSuccess()) {
                                // save options
                                SmartLockHelper.getInstance(SmartLockHiddenActivity.this)
                                        .saveSmartLockOptions(mSmartLockOptions);
                                // credential saved
                                mStatusObservable.onNext(new RxStatus(status));
                                mStatusObservable.onCompleted();
                                finish();
                            }
                            else if(status.hasResolution()) {
                                // Try to resolve the save request. This will prompt the user if
                                // the credential is new.
                                try {
                                    status.startResolutionForResult(SmartLockHiddenActivity.this, RC_SAVE);
                                } catch (IntentSender.SendIntentException e) {
                                    // Could not resolve the request
                                    Throwable throwable = new Throwable(new Throwable(e.getMessage()));
                                    mStatusObservable.onError(throwable);
                                    finish();
                                }
                            }
                            else {
                                // request has no resolution
                                mStatusObservable.onCompleted();
                                finish();
                            }
                        }
                    });
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
                                SmartLockHelper.getInstance(SmartLockHiddenActivity.this)
                                        .saveSmartLockOptions(options);
                                // credential saved
                                mAccountObservableIntern.onNext(mCurrentAccount);
                                mAccountObservableIntern.onCompleted();
                                // reset current account
                                mCurrentAccount = null;
                                finish();
                            }
                            else if(status.hasResolution()) {
                                // Try to resolve the save request. This will prompt the user if
                                // the credential is new.
                                try {
                                    status.startResolutionForResult(SmartLockHiddenActivity.this, RC_SAVE_INTERN);
                                } catch (IntentSender.SendIntentException e) {
                                    // Could not resolve the request
                                    Throwable throwable = new Throwable(new Throwable(e.getMessage()));
                                    mAccountObservableIntern.onError(throwable);
                                    // reset current account
                                    mCurrentAccount = null;
                                    finish();
                                }
                            }
                            else {
                                // request has no resolution
                                mAccountObservableIntern.onCompleted();
                                // reset current account
                                mCurrentAccount = null;
                                finish();
                            }
                        }
                    });

            return mAccountObservableIntern;
        }

        /**
         * Delete credential
         */
        private void deleteCredential() {
            Auth.CredentialsApi.delete(mCredentialsApiClient, mCredential).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    mStatusObservable.onNext(new RxStatus(status));
                    mStatusObservable.onCompleted();
                    finish();
                }
            });
        }

        /**
         * Disable auto sign in
         */
        private void disableAutoSignIn() {
            Auth.CredentialsApi.disableAutoSignIn(mCredentialsApiClient).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(@NonNull Status status) {
                    mStatusObservable.onNext(new RxStatus(status));
                    mStatusObservable.onCompleted();
                    finish();
                }
            });
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == RC_READ) {
                if (resultCode == Activity.RESULT_OK) {
                    Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                    onCredentialRetrieved(credential);
                } else {
                    // The user must create an account or sign in manually.
                    mAccountObservable.onError(new Throwable(getString(R.string.status_canceled_request_credential)));
                    finish();
                }
            }
            else if (requestCode == RC_SAVE) {
                if (resultCode == RESULT_OK) {
                    // credentials saved
                    mStatusObservable.onNext(new RxStatus(
                            CommonStatusCodes.SUCCESS,
                            getString(R.string.status_success_credential_saved_message)
                    ));
                    mStatusObservable.onCompleted();
                    finish();
                } else {
                    // cancel by user
                    mStatusObservable.onNext(new RxStatus(
                            CommonStatusCodes.CANCELED,
                            getString(R.string.status_canceled_credential_saved_message)
                    ));
                    mStatusObservable.onCompleted();
                    finish();
                }
            }
            else if (requestCode == RC_SAVE_INTERN) {
                if (resultCode == RESULT_OK) {
                    // credentials saved
                    mAccountObservableIntern.onNext(mCurrentAccount);
                    mAccountObservableIntern.onCompleted();
                    // reset current account
                    mCurrentAccount = null;
                    finish();
                } else {
                    // cancel by user
                    mAccountObservableIntern.onNext(mCurrentAccount);
                    mAccountObservableIntern.onCompleted();
                    // reset current account
                    mCurrentAccount = null;
                    finish();
                }
            }
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            // launch action
            switch (mCredentialAction) {
                case REQUEST:
                    requestCredential();
                    break;
                case REQUEST_AND_AUTO_SIGN_IN:
                    requestCredentialAndAutoSignIn();
                    break;
                case SAVE:
                    saveCredential();
                    break;
                case SAVE_WITH_OPTIONS:
                    saveCredentialWithOptions();
                    break;
                case DELETE:
                    deleteCredential();
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
            if(mCredentialAction == REQUEST)
                mAccountObservable.onError(throwable);
            else
                mStatusObservable.onError(throwable);

            finish();
        }
    }

    enum CredentialAction {
        REQUEST, REQUEST_AND_AUTO_SIGN_IN, SAVE, SAVE_WITH_OPTIONS, DELETE, DISABLE_AUTO_SIGN_IN
    }
}
