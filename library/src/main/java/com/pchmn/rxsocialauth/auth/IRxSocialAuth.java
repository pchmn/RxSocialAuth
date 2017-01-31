package com.pchmn.rxsocialauth.auth;


import rx.Observable;
import rx.subjects.PublishSubject;

public interface IRxSocialAuth {

    PublishSubject<RxAccount> signIn();

    Observable<RxStatus> signOut();

    enum AuthAction {
        SIGN_IN, SILENT_SIGN_IN, SIGN_OUT, REVOKE_ACCESS
    }

    interface IHiddenActivity {
        void signIn();
        void signOut();
        void verifySmartLockIsEnabled(RxAccount account);
        void saveCurrentUser(RxAccount account);
        void deleteCurrentUser();
    }
}
