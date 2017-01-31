package com.pchmn.rxsocialauth.smartlock;


public class GoogleOptions extends SmartLockOptions {
    public String clientId;

    public GoogleOptions(boolean requestEmail, boolean requestProfile, String clientId) {
        super(requestEmail, requestProfile);
        this.clientId = clientId;
    }
}
