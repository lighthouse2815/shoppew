package com.shoppew.auth.email;

public interface AuthenticationEmailGateway {

    void sendEmailVerification(String recipient, String rawToken);

    void sendPasswordReset(String recipient, String rawToken);
}
