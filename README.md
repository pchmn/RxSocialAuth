# RxSocialAuth
Android RxJava library for Social auth (Google, Facebook) and Smart Lock For Passwords

## Setup

## Usage

### Getting started

In order to use this library with Google Sign-In and Facebook Login, you need to prepare your project.

#### Google Sign-In
Follow the official guide : https://developers.google.com/identity/sign-in/android/start-integrating

In short : 
* Put your `google-services.json` into the `/app` directory of your project
* Add the dependency `classpath 'com.google.gms:google-services:3.0.0'`  to your project-level `build.gradle`
* Add the plugin `apply plugin: 'com.google.gms.google-services'` at the end of your app-level `build.gradle`
* Add the dependency `compile 'com.google.android.gms:play-services-auth:10.0.1'`  to your app-level `build.gradle`

#### Facebook Login
* Create an app ID on the facebook developer website : https://developers.facebook.com/ and activate Facebook Login on the console administration.
* Add your facebook app ID in the strings.xml (or keys.xml) of your project like this : 
```java
<string name="facebook_app_id">you_app_id</string>
```
* Modify your Manifest.xml like this : 
```java
<application android:label="@string/app_name" ...>
    ...
    <meta-data
            android:name="com.facebook.sdk.ApplicationId"
            android:value="@string/facebook_app_id" />
    ...
</application>
```
* You don't have to add the facebook SDK in your project, it is already included in this library

If you have trouble when trying to authenticate users, maybe you have to add the users you're trying to authenticate in your test users. See http://stackoverflow.com/questions/41861564/server-error-code-1675030-message-error-performing-query


### Shared classes

#### RxAccount
This class represents a social account and has these methods :
* `String getId()`
* `String getAccessToken()`
* `String getEmail()`
* `String getFirstname()`
* `String getLastname()`
* `String getDisplayName()`
* `Uri getPhotoUri()`

These methods can return a null object according to the permissions you asked or didn't ask when signed in.

#### RxStatus
This class represents the status of a request and has these methods : 
* `int getStatusCode()`
* `String getMessage()`
* `boolean isSuccess()`
* `boolean isCanceled()`


### Google Sign-In
Create a `RxGoogleAuth` object using the `RxGoogleAuth.Builder` builder.
```java
// build RxGoogleAuth object
RxGoogleAuth rxGoogleAuth = new RxGoogleAuth.Builder(this)
        .requestIdToken(getString(R.string.oauth_google_key))
        .requestEmail()
        .requestProfile()
        .disableAutoSignIn()
        .enableSmartLock(true)
        .build();
```
You can configure the builder with these methods : 
* `requestEmail()` : Request the email 
* `requestProfile()` : Request the profile (mandatory to get the profile picture) 
* `requestIdToken(String clientId)` : Request an id token for authenticated users (mandatory to get the profile picture) 
* `disableAutoSignIn()` : Clear the account previously selected by the user, so the user will have to pick an account
* `enableSmartLock(boolean enable)` : Enable or disable smart lock password. If enabled, it will save automatically the credential in Smart Lock For Password

Then you can use these `signIn()`, `silentSignIn(Credential credential)`, `signOut()` and `revokeAccess()` methods of the `RxGoogleAuth` object.

With `signIn()` and `silentSignIn(Credential credential)` methods, the observer receive a `RxAccount` object in case of success.
```java
// sign in
rxGoogleAuth.signIn()
        .subscribe(rxAccount -> {
            // user is signed in
            // use the rxAccount object as you want
            Log.d(TAG, "name: " + rxAccount.getDisplayName());
        }, 
        throwable -> {
            Log.e(TAG, throwable.getMessage());
        });


// silent sign in
// you have to pass a credential object in order to silent sign in
rxGoogleAuth.silentSignIn(Credential credential)
        .subscribe(rxAccount -> {
            // user is signed in
            // use the rxAccount object as you want
            Log.d(TAG, "name: " + rxAccount.getDisplayName());
        }, 
        throwable -> {
            Log.e(TAG, throwable.getMessage());
        });
```

With `signOut()` and `revokeAccess()` methods, the observer receive a `RxStatus` object in case of success.
```java
// sign out
rxGoogleAuth.signOut()
        .subscribe(rxStatus -> {
            if(rxStatus.isSuccess())
                // user is signed out
        }, 
        throwable -> {
            Log.e(TAG, throwable.getMessage());
        });
       
// revoke access
rxGoogleAuth.revokeAccess()
        .subscribe(rxStatus -> {
            if(rxStatus.isSuccess())
                // access is revoked
        }, 
        throwable -> {
            Log.e(TAG, throwable.getMessage());
        }); 
```
