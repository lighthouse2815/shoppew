package com.shoppew.auth.email;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.email.delivery-enabled", havingValue = "false", matchIfMissing = true)
class DisabledAuthenticationEmailGateway implements AuthenticationEmailGateway {

    @Override
    public void sendEmailVerification(String recipient, String rawToken) {
        // Delivery is intentionally disabled by environment configuration.
    }

    @Override
    public void sendPasswordReset(String recipient, String rawToken) {
        // Delivery is intentionally disabled by environment configuration.
    }
}
