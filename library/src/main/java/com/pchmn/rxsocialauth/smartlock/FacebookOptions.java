package com.pchmn.rxsocialauth.smartlock;


public class FacebookOptions extends SmartLockOptions {
    public int photoWidth;
    public int photoHeight;

    public FacebookOptions(boolean requestEmail, boolean requestProfile, int photoWidth, int photoHeight) {
        super(requestEmail, requestProfile);
        this.photoWidth = photoWidth;
        this.photoHeight = photoHeight;
    }
}
