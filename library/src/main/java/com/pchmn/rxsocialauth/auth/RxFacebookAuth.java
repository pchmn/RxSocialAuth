package com.pchmn.rxsocialauth.auth;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.pchmn.rxsocialauth.R;
import com.pchmn.rxsocialauth.smartlock.FacebookOptions;
import com.pchmn.rxsocialauth.smartlock.RxSmartLockPassword;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

public class RxFacebookAuth implements IRxSocialAuth {

    private static final String TAG = RxFacebookAuth.class.toString();
    // context
    private Context mContext;
    // options
    private static boolean mRequestEmail;
    private static boolean mRequestProfile;
    private static int mPhotoWidth;
    private static int mPhotoHeight;
    private static boolean mEnableSmartLock;
    // facebook auth action
    private static AuthAction mAuthAction;
    // publisher
    private static PublishSubject<RxAccount> mAccountObserver;
    private static PublishSubject<RxStatus> mStatusObserver;


    /**
     * Construct a RxFacebookAuth object from a RxFacebookAuth.Builder
     *
     * @param builder the builder
     */
    private RxFacebookAuth(Builder builder) {
        mContext = builder.context;
        mRequestEmail = builder.requestEmail;
        mRequestProfile = builder.requestProfile;
        mPhotoWidth = builder.photoWidth;
        mPhotoHeight = builder.photoHeight;
        mEnableSmartLock = builder.enableSmartLock;
    }

    /**
     * Builder inner class for RxFacebookAuth
     */
    public static class Builder {
        private Context context;
        private boolean requestEmail = false;
        private boolean requestProfile = false;
        private int photoWidth = 50;
        private int photoHeight = 50;
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
     * Facebook sign in by launching a FacebookAuthHiddenActivity
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
     * Facebook sign out by launching a FacebookAuthHiddenActivity
     *
     * @return an Observable
     */
    @Override
    public Observable<RxStatus> signOut() {
        mStatusObserver = PublishSubject.create();
        mAuthAction = AuthAction.SIGN_OUT;
        mContext.startActivity(getIntent());
        return null;
    }

    /**
     * Get intent for FacebookAuthHiddenActivity
     *
     * @return the intent
     */
    private Intent getIntent() {
        Intent intent = new Intent(mContext, FacebookAuthHiddenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /**
     * Inner activity that will do the work
     */
    public static class FacebookAuthHiddenActivity extends AppCompatActivity implements IHiddenActivity {

        // facebook auth
        private CallbackManager mCallbackManager;
        private ProfileTracker mProfileTracker;

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // initialize
            FacebookSdk.sdkInitialize(getApplicationContext());
            mCallbackManager = CallbackManager.Factory.create();

            // callback
            LoginManager.getInstance().registerCallback(mCallbackManager,
                    new FacebookCallback<LoginResult>() {
                        @Override
                        public void onSuccess(final LoginResult loginResult) {
                            if(Profile.getCurrentProfile() == null) {
                                mProfileTracker = new ProfileTracker() {
                                    @Override
                                    protected void onCurrentProfileChanged(Profile profile, Profile profile2) {
                                        // profile2 is the new profile
                                        Profile.setCurrentProfile(profile2);
                                        mProfileTracker.stopTracking();
                                        handleLogInResult(loginResult);
                                    }
                                };
                                // no need to call startTracking() on mProfileTracker
                                // because it is called by its constructor, internally.
                            }
                            else {
                                handleLogInResult(loginResult);
                            }
                        }

                        @Override
                        public void onCancel() {
                            Throwable throwable = new Throwable(
                                    getString(R.string.status_canceled_by_user));
                            mAccountObserver.onError(throwable);
                            finish();
                        }

                        @Override
                        public void onError(FacebookException exception) {
                            // fix Error occurred when trying to propagate error to Observer.onError
                            Throwable throwable = new Throwable(new Throwable(exception.getMessage()));
                            mAccountObserver.onError(throwable);
                            finish();
                        }
                    });

            // launch action
            if(mAuthAction == AuthAction.SIGN_IN)
                signIn();
            else if(mAuthAction == AuthAction.SIGN_OUT)
                signOut();
        }

        /**
         * Facebook sign in
         */
        @Override
        public void signIn() {
            LoginManager.getInstance().logInWithReadPermissions(this, getFbPermissions());
        }

        /**
         * Facebook sign out
         */
        @Override
        public void signOut() {
            LoginManager.getInstance().logOut();
            // delete current user
            deleteCurrentUser();
            mStatusObserver.onNext(new RxStatus(
                    CommonStatusCodes.SUCCESS,
                    getString(R.string.status_success_log_out_message)
            ));
            mStatusObserver.onCompleted();
            finish();
        }

        /**
         * Get facebook permissions asked by the developer
         *
         * @return the list of permissions
         */
        private List<String> getFbPermissions() {
            List<String> permissions = new ArrayList<>();
            if(mRequestEmail)
                permissions.add("email");
            if(mRequestProfile)
                permissions.add("public_profile");
            return permissions;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            // facebook auth
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
        }

        /**
         * Handle the Facebook login result
         *
         * @param loginResult the login result
         */
        private void handleLogInResult(final LoginResult loginResult) {
            GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(),
                    new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(JSONObject object, GraphResponse response) {
                            try {

                                // get current fb profile
                                Profile profile = Profile.getCurrentProfile();
                                if(profile != null) {
                                    // create social account
                                    RxAccount account = new RxAccount();
                                    account.setProvider(IdentityProviders.FACEBOOK);
                                    // add info
                                    account.setId(profile.getId());
                                    account.setAccessToken(loginResult.getAccessToken().getToken());
                                    account.setFirstname(profile.getFirstName());
                                    account.setLastname(profile.getLastName());
                                    account.setDisplayName(profile.getName());
                                    account.setPhotoUri(profile.getProfilePictureUri(mPhotoWidth, mPhotoHeight));
                                    account.setEmail(object.has("email")? object.getString("email") : "");

                                    verifySmartLockIsEnabled(account);
                                }
                                else {
                                    Throwable throwable =
                                            new Throwable(getString(R.string.status_error_sign_in_message));
                                    mAccountObserver.onError(throwable);
                                    finish();
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                                // fix Error occurred when trying to propagate error to Observer.onError
                                Throwable throwable = new Throwable(new Throwable(e.getMessage()));
                                mAccountObserver.onError(throwable);
                                finish();
                            }
                        }
                    });
            // launch graph request
            Bundle parameters = new Bundle();
            parameters.putString("fields", "email,id,first_name,last_name,name");
            request.setParameters(parameters);
            request.executeAsync();
        }

        /**
         * Verify if smart lock is enabled, save the account if it is
         *
         * @param account the social account to save
         */
        @Override
        public void verifySmartLockIsEnabled(final RxAccount account) {
            if(account.getEmail() == null || account.getEmail().equals("")) {
                mAccountObserver.onError(new Throwable(getString(R.string.status_canceled_email_facebook)));
                finish();
            }
            else if(mEnableSmartLock) {
                Credential credential = new Credential.Builder(account.getEmail())
                        .setAccountType(IdentityProviders.FACEBOOK)
                        .setName(account.getDisplayName())
                        .setProfilePictureUri(account.getPhotoUri())
                        .build();

                // save options
                FacebookOptions options =
                        new FacebookOptions(mRequestEmail, mRequestProfile, mPhotoWidth, mPhotoHeight);

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
                                    // fix Error occurred when trying to propagate error to Observer.onError
                                    Throwable throwable = new Throwable(new Throwable(status.getMessage()));
                                    mAccountObserver.onError(throwable);
                                    finish();
                                }
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                mAccountObserver.onError(new Throwable(throwable));
                                finish();
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
    }
}
