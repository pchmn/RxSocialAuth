package com.pchmn.rxsocialauth.auth;


import android.net.Uri;

import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

public class RxAccount {

    private String id;
    private String accessToken;
    private String email;
    private String firstname;
    private String lastname;
    private String displayName;
    private Uri photoUri;
    private String provider;

    public RxAccount() {
    }

    public RxAccount(String id, String accessToken, String email, String firstname, String lastname, String displayName, Uri photoUri, String provider) {
        this.id = id;
        this.accessToken = accessToken;
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
        this.displayName = displayName;
        this.photoUri = photoUri;
        this.provider = provider;
    }

    public RxAccount(GoogleSignInAccount gsa) {
        this.id = gsa.getId();
        this.accessToken = gsa.getIdToken();
        this.email = gsa.getEmail();
        this.firstname = gsa.getGivenName();
        this.lastname = gsa.getFamilyName();
        this.displayName = gsa.getDisplayName();
        this.photoUri = gsa.getPhotoUrl();
        this.provider = IdentityProviders.GOOGLE;
    }

    public RxAccount(RxAccount account) {
        this.provider = account.getProvider();
        this.id = account.getId();
        this.accessToken = account.getAccessToken();
        this.email = account.getEmail();
        this.firstname = account.getFirstname();
        this.lastname = account.getLastname();
        this.displayName = account.getDisplayName();
        this.photoUri = account.getPhotoUri();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Uri getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(Uri photoUri) {
        this.photoUri = photoUri;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
