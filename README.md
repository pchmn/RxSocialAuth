# RxSocialAuth
Android RxJava library for Social auth (Google, Facebook) and Smart Lock For Passwords

[![Release](https://jitpack.io/v/pchmn/RxSocialAuth.svg)](https://jitpack.io/#pchmn/RxSocialAuth)

## Setup
To use this library your `minSdkVersion` must be >= 15.

In your project level build.gradle :

```java
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```

In your app level build.gradle :

```java
dependencies {
    compile 'com.github.pchmn:RxSocialAuth:1.0.1-beta'
}
```

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
* `String getProvider()`
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
// 'this' represents a Context
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
* `enableSmartLock(boolean enable)` : Enable or disable Smart Lock For Passwords. If enabled, it will save automatically the credential in Smart Lock For Passwords


#### Sign in and silent sign in
With `signIn()` and `silentSignIn(Credential credential)` methods, the observer receives a `RxAccount` object in case of success.

I think I found a bug in the Google Sign-In API with the [`silentSignIn()`](https://developers.google.com/android/reference/com/google/android/gms/auth/api/signin/GoogleSignInApi.html#silentSignIn(com.google.android.gms.common.api.GoogleApiClient)) method. If the user you try to silent sign in doesn't have a Google+ account, no matter if you set the [`requestProfile()`](https://developers.google.com/android/reference/com/google/android/gms/auth/api/signin/GoogleSignInOptions.Builder.html#requestProfile()) options, you won't have his profile.
```java
// sign in
rxGoogleAuth.signIn()
        .subscribe(rxAccount -> {
            // user is signed in
            // use the rxAccount object as you want
            Log.d(TAG, "name: " + rxAccount.getDisplayName());
            Log.d(TAG, "email: " + rxAccount.getEmail());
            
        }, throwable -> {
            Log.e(TAG, throwable.getMessage());
        });


// silent sign in
// you have to pass a credential object in order to silent sign in
rxGoogleAuth.silentSignIn(Credential credential)
        .subscribe(rxAccount -> {
            // user is signed in
            // use the rxAccount object as you want
            Log.d(TAG, "name: " + rxAccount.getDisplayName());
            Log.d(TAG, "email: " + rxAccount.getEmail());
            
        }, throwable -> {
            Log.e(TAG, throwable.getMessage());
        });
```

#### Sign out and revoke access
With `signOut()` and `revokeAccess()` methods, the observer receives a `RxStatus` object in case of success.
```java
// sign out
rxGoogleAuth.signOut()
        .subscribe(rxStatus -> {
            if(rxStatus.isSuccess()) {
                // user is signed out
            }
            
        }, throwable -> {
            Log.e(TAG, throwable.getMessage());
        });
       
       
// revoke access
rxGoogleAuth.revokeAccess()
        .subscribe(rxStatus -> {
            if(rxStatus.isSuccess()) {
                // access is revoked
            }
            
        }, throwable -> {
            Log.e(TAG, throwable.getMessage());
        }); 
```


### Facebook Login
Create a `RxFacebookAuth` object using the `RxFacebookAuth.Builder` builder.
```java
// build RxFacebookAuth object
// 'this' represents a Context
RxFacebookAuth rxFacebookAuth = new RxFacebookAuth.Builder(this)
        .enableSmartLock(true)
        .requestEmail()
        .requestProfile()
        .requestPhotoSize(200, 200)
        .build();
```
You can configure the builder with these methods : 
* `requestEmail()` : Request the email 
* `requestProfile()` : Request the profile (mandatory to get the profile picture) 
* `requestPhotoSize(int width, int height)` : Request a specific photo size, cause request profile
* `enableSmartLock(boolean enable)` : Enable or disable Smart Lock For Passwords. If enabled, it will save automatically the credential in Smart Lock For Passwords

#### Sign in 
Like with Google Sign-In, the `signIn()` method will return an `Observable<RxAccount>`. And the observer will receive a `RxAccount` object in case of success.
```java
// sign in
rxFacebookAuth.signIn()
        .subscribe(rxAccount -> {
            // user is signed in
            // use the rxAccount object as you want
            Log.d(TAG, "name: " + rxAccount.getDisplayName());
            Log.d(TAG, "email: " + rxAccount.getEmail());
            
        }, throwable -> {
            Log.e(TAG, throwable.getMessage());
        });
```

#### Sign out 
With `signOut()` method, the observer receives a `RxStatus` object in case of success.
```java
// sign out
rxFacebookAuth.signOut()
        .subscribe(rxStatus -> {
            if(rxStatus.isSuccess()) {
                // user is signed out
            }
            
        }, throwable -> {
            Log.e(TAG, throwable.getMessage());
        });
```

### RxAuthSocial
This class permits to access and sign out the current account without knowing if it is a Google account or a Facebook account. For example it is useful when you want to sign out the current user but you don't know if he/she is connected with a Google or a Facebook account.

This class uses the singleton pattern.

#### Sign out
This method will signed out the current user from the app, and from the current provider. It will also disable auto sign in for Smart Lock for Passwords and for the current provider. So the user will be able to pick an other account when using the authentication method again.

```java
// sign out
// 'this' represents a context
RxSocialAuth.getInstance(this).signOut()
        .subscribe(rxStatus -> {
            if(rxStatus.isSuccess()) {
                // user is signed out
            }
            
            }, throwable -> {
                    Log.e(TAG, throwable.getMessage());
            });
```

#### Get current user
This method return a `RxAccount` object representing the current user in the app. If the user is signed out, the `RxAccount` object will be null.

```java
// current user
// 'this' represents a context
RxAccount currentUser = RxSocialAuth.getInstance(this).getCurrentUser();
```


### Smart Lock For Passwords
Create a `RxSmartLockPassword` object using the `RxSmartLockPassword.Builder` builder.
```java
// build RxSmartLockPassword object
// 'this' represents a Context
RxSmartLockPassword rxSmartLockPassword = new RxSmartLockPassword.Builder(this)
        .disableAutoSignIn()
        .setAccountTypes(IdentityProviders.GOOGLE, IdentityProviders.FACEBOOK)
        .build()
```
You can configure the builder with these methods : 
* `setAccountTypes(String... accountTypes)` : Set the account types (identity providers)
* `setPasswordLoginSupported(boolean supported)` : Enable returning credentials with a password, that is verified by the application
* `disableAutoSignIn()` : Disable auto sign 

#### Retrieve a user's stored credentials
Automatically sign users into your app by using the Credentials API to request and retrieve stored credentials for your users.

If you want to retrieve a Facebook account credential, for example, you have to call `.setAccountTypes(IdentityProviders.FACEBOOK)` on the builder of your `RxSmartLockPassword` object before.

##### Request credential
To retrieve user's stored credentials, use the `requestCredential()` method on a `RxSmartLockPassword` object. This method will emit a `Observable<CredentialRequestResult>`. 

```java
// request credential
rxSmartLockPassword.requestCredential()
        .subscribe(credentialRequestResult -> {
            if (credentialRequestResult.getStatus().isSuccess()) {
                onCredentialRetrieved(credentialRequestResult.getCredential());
            } 
            else {
                resolveResult(credentialRequestResult.getStatus());
            }
            
        }, throwable -> {
            Log.e(TAG, throwable.getMessage());
        });
```

##### Request credential and auto sign in
To retrieve user's stored credentials and automatically sign in the user with found credentials, use the `requestCredentialAndAutoSignIn()` method. It will emit a `Observable<Object>`.

With this method you don't have to handle different cases. No matter if there is no stored credential, if there is only one stored credential, or if there are multiple stored credentials, the `requestCredentialAndAutoSignIn()` method will do all the work.

* If there is only one stored credential, or if the user picks one of the multiple stored credentials, this method will catch it :
    * If the account type is `Google` or `Facebook`, the user will be automatically sign in according to the provider, and the observer will receive a `RxAccount` object in case of success. If it fails a `Throwable` will be emitted.
    * If the account type is null, this is a `LoginPassword` credential. In this case, the observer will receive a `Credential` object, containing the id and the password, and you'll have to authenticate the user manually with the credential. 

* If the user cancels or if there is no stored credential, a `Throwable` will be emitted.

```java
// request credential and auto sign in
rxSmartLockPassword.requestCredentialAndAutoSignIn()
        .subscribe(o -> {
            if(o instanceof RxAccount) {
                // user is signed in using google or facebook
                RxAccount rxAccount = (RxAccount) o;
                Log.d(TAG, "provider: " + rxAccount.getProvider());
                Log.d(TAG, "email: " + rxAccount.getEmail());
             }
             else if(o instanceof Credential) {
                // credential contains login and password
                Credential credential = (Credential) o;
                // sign in manually
                signInWithLoginPassword(credential.getId(), credential.getPassword());
             }

        }, throwable -> {
            Log.e(TAG, throwable.getMessage());
        });
        
        
private void signInWithLoginPassword(String login, String password) {
    // authenticate the user like you want
}
```

#### Store a user's credentials
After users successfully sign in, create accounts, or change passwords, allow them to store their credentials to automate future authentication in your app.

To store a user's credentials, use the `saveCredential(Credential credential)` method on a `RxSmartLockPassword` object. In case of success the observer will receive a `RxStatus` object.

If you used `RxGoogleAuth` or `RxFacebookAuth` with the `enableSmartLock(true)` option, the credential is stored automatically, and you don't have to use this method.

```java
// save credential
rxSmartLockPassword.saveCredential(Credential credential)
        .subscribe(rxStatus -> {
            if(rxStatus.isSuccess()) {
                // credential saved
            }
            
        }, throwable -> {
            Log.e(TAG, throwable.getMessage());
        });
```

#### Delete stored credentials

To delete a stored credential, use the `deleteCredential(Credential credential)` method on a `RxSmartLockPassword` object. In case of success the observer will receive a `RxStatus` object.

```java
// delete credential
rxSmartLockPassword.deleteCredential(Credential credential)
        .subscribe(rxStatus -> {
            if(rxStatus.isSuccess()) {
                // credential deleted
            }
            
        }, throwable -> {
            Log.e(TAG, throwable.getMessage());
        });
```
## Sample
A sample app with some use cases of the library is available on this [link](https://github.com/pchmn/RxSocialAuth/tree/master/sample)

## Status
This library is currently in beta

## Credits
This library was greatly inspired by [SocialLoginManager](https://github.com/jaychang0917/SocialLoginManager)

## License
```
Copyright 2017 pchmn

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
