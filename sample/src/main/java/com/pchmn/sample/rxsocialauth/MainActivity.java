package com.pchmn.sample.rxsocialauth;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.pchmn.rxsocialauth.auth.RxAccount;
import com.pchmn.rxsocialauth.auth.RxSocialAuth;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.toString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        RxAccount currentUser = RxSocialAuth.getInstance(this).getCurrentUser();
        // log info
        Log.d(TAG, "current user userId: " + currentUser.getId());
        Log.d(TAG, "current user photoUrl: " +
                (currentUser.getPhotoUri() != null? currentUser.getPhotoUri().toString(): ""));
        Log.d(TAG, "current user accessToken: " + currentUser.getAccessToken());
        Log.d(TAG, "current user firstname: " + currentUser.getFirstname());
        Log.d(TAG, "current user lastname: " + currentUser.getLastname());
        Log.d(TAG, "current user name: " + currentUser.getDisplayName());
        Log.d(TAG, "current user email: " + currentUser.getEmail());
    }

    @OnClick(R.id.sign_out_button)
    public void signOut() {
        RxSocialAuth.getInstance(this).signOut()
                .subscribe(rxStatus -> {
                    if(rxStatus.isSuccess()) {
                        // log info
                        Log.d(TAG, "current user is null: " +
                                (RxSocialAuth.getInstance(this).getCurrentUser() == null));

                        // go to auth activity
                        startActivity(new Intent(MainActivity.this, AuthActivity.class));
                        finish();
                    }
                }, throwable -> {
                    Log.e(TAG, throwable.getMessage());
                });
    }
}
