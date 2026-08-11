package com.shoppew.auth.service;

import com.shoppew.auth.entity.AuthActionTokenType;

record AuthActionIssuedEvent(
        String recipient,
        String rawToken,
        AuthActionTokenType type) {}
