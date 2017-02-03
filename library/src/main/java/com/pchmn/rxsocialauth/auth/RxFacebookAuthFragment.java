package com.pchmn.rxsocialauth.auth;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

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
import com.pchmn.rxsocialauth.common.RxAccount;
import com.pchmn.rxsocialauth.common.RxStatus;
import com.pchmn.rxsocialauth.smartlock.FacebookOptions;
import com.pchmn.rxsocialauth.smartlock.RxSmartLockPasswords;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import rx.functions.Action1;
import rx.subjects.PublishSubject;

public class RxFacebookAuthFragment extends Fragment {

    public static final String TAG = RxFacebookAuthFragment.class.toString();
    // activity
    private FragmentActivity mActivity;
    // options
    private boolean mRequestEmail;
    private boolean mRequestProfile;
    private int mPhotoWidth;
    private int mPhotoHeight;
    private boolean mEnableSmartLock;
    // facebook auth
    private CallbackManager mCallbackManager;
    private ProfileTracker mProfileTracker;
    // subjects
    private PublishSubject<RxAccount> mAccountSubject;
    private PublishSubject<RxStatus> mStatusSubject;

    public static RxFacebookAuthFragment newInstance(RxFacebookAuth.Builder builder) {
        RxFacebookAuthFragment rxFacebookAuthFragment = new RxFacebookAuthFragment();
        rxFacebookAuthFragment.setRequestEmail(builder.requestEmail);
        rxFacebookAuthFragment.setRequestProfile(builder.requestProfile);
        rxFacebookAuthFragment.setPhotoWidth(builder.photoWidth);
        rxFacebookAuthFragment.setPhotoHeight(builder.photoHeight);
        rxFacebookAuthFragment.setEnableSmartLock(builder.enableSmartLock);
        return rxFacebookAuthFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();

        // initialize
        FacebookSdk.sdkInitialize(mActivity.getApplicationContext());
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
                        mAccountSubject.onError(throwable);
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        // fix Error occurred when trying to propagate error to Observer.onError
                        Throwable throwable = new Throwable(new Throwable(exception.getMessage()));
                        mAccountSubject.onError(throwable);
                    }
                });
    }

    /**
     * Facebook sign in
     */
    public void signIn(PublishSubject<RxAccount> accountSubject) {
        mAccountSubject = accountSubject;
        LoginManager.getInstance().logInWithReadPermissions(this, getFbPermissions());
    }


    /**
     * Facebook sign out
     */
    public void signOut(PublishSubject<RxStatus> statusSubject) {
        LoginManager.getInstance().logOut();
        // delete current user
        deleteCurrentUser();
        statusSubject.onNext(new RxStatus(
                CommonStatusCodes.SUCCESS,
                getString(R.string.status_success_log_out_message)
        ));
        statusSubject.onCompleted();
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
                                account.setEmail(object.has("email")? object.getString("email") : null);

                                verifySmartLockIsEnabled(account);
                            }
                            else {
                                Throwable throwable =
                                        new Throwable(getString(R.string.status_error_sign_in_message));
                                mAccountSubject.onError(throwable);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            // fix Error occurred when trying to propagate error to Observer.onError
                            Throwable throwable = new Throwable(new Throwable(e.getMessage()));
                            mAccountSubject.onError(throwable);
                        }
                    }
                });
        // launch graph request
        Bundle parameters = new Bundle();
        parameters.putString("fields", "email,id,first_name,last_name,name");
        request.setParameters(parameters);
        request.executeAsync();
    }

    private void verifySmartLockIsEnabled(final RxAccount account) {
        if(account.getEmail() == null) {
            mAccountSubject.onError(new Throwable(getString(R.string.status_canceled_email_facebook)));
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

    public void setEnableSmartLock(boolean mEnableSmartLock) {
        this.mEnableSmartLock = mEnableSmartLock;
    }

    public void setRequestEmail(boolean mRequestEmail) {
        this.mRequestEmail = mRequestEmail;
    }

    public void setRequestProfile(boolean mRequestProfile) {
        this.mRequestProfile = mRequestProfile;
    }

    public void setPhotoWidth(int mPhotoWidth) {
        this.mPhotoWidth = mPhotoWidth;
    }

    public void setPhotoHeight(int mPhotoHeight) {
        this.mPhotoHeight = mPhotoHeight;
    }
}
