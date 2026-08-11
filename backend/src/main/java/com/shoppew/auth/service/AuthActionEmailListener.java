package com.shoppew.auth.service;

import com.shoppew.auth.email.AuthenticationEmailGateway;
import com.shoppew.auth.entity.AuthActionTokenType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class AuthActionEmailListener {

    private final AuthenticationEmailGateway gateway;

    AuthActionEmailListener(AuthenticationEmailGateway gateway) {
        this.gateway = gateway;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onIssued(AuthActionIssuedEvent event) {
        if (event.type() == AuthActionTokenType.EMAIL_VERIFICATION) {
            gateway.sendEmailVerification(event.recipient(), event.rawToken());
        } else {
            gateway.sendPasswordReset(event.recipient(), event.rawToken());
        }
    }
}
