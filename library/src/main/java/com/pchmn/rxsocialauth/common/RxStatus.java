package com.pchmn.rxsocialauth.common;


import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

public class RxStatus {

    private int statusCode;
    private String message;
    private boolean success;

    public RxStatus() {
    }

    public RxStatus(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
        this.success = statusCode == CommonStatusCodes.SUCCESS;
    }

    public RxStatus(Status status) {
        this.statusCode = status.getStatusCode();
        this.message = status.getStatusMessage();
        this.success = status.isSuccess();
    }

    public RxStatus(RxStatus status) {
        this.statusCode = status.getStatusCode();
        this.message = status.getMessage();
        this.success = status.isSuccess();
    }

    @Override
    public String toString() {
        return "{status: " + this.statusCode + ", message: " + this.message + "}";
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isCanceled() {
        return this.statusCode == CommonStatusCodes.CANCELED;
    }
}
