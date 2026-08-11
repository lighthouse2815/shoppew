package com.shoppew.auth.controller;

import com.shoppew.auth.dto.AuthResponse;
import com.shoppew.auth.dto.AuthUserResponse;
import com.shoppew.auth.dto.LoginRequest;
import com.shoppew.auth.dto.RegisterRequest;
import com.shoppew.auth.dto.SessionResponse;
import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.auth.service.AuthService;
import com.shoppew.auth.web.AuthCookieService;
import com.shoppew.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService cookieService;
    private final AuthenticatedUser authenticatedUser;
    private final Clock clock;

    public AuthController(
            AuthService authService,
            AuthCookieService cookieService,
            AuthenticatedUser authenticatedUser,
            Clock clock) {
        this.authService = authService;
        this.cookieService = cookieService;
        this.authenticatedUser = authenticatedUser;
        this.clock = clock;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        AuthService.AuthResult result = authService.register(request, servletRequest.getHeader(HttpHeaders.USER_AGENT));
        setRefreshCookieIfPresent(result, servletResponse);
        return ApiResponse.success(result.response(), clock);
    }

    @PostMapping("/login")
    ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        AuthService.AuthResult result = authService.login(request, servletRequest.getHeader(HttpHeaders.USER_AGENT));
        setRefreshCookieIfPresent(result, servletResponse);
        return ApiResponse.success(result.response(), clock);
    }

    @PostMapping("/refresh")
    ApiResponse<AuthResponse> refresh(
            @CookieValue(name = AuthCookieService.REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        AuthService.AuthResult result = authService.refresh(refreshToken, servletRequest.getHeader(HttpHeaders.USER_AGENT));
        setRefreshCookieIfPresent(result, servletResponse);
        return ApiResponse.success(result.response(), clock);
    }

    @PostMapping("/logout")
    ApiResponse<Map<String, Boolean>> logout(
            @CookieValue(name = AuthCookieService.REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse servletResponse) {
        authService.logout(refreshToken);
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, cookieService.clearRefreshCookie().toString());
        return ApiResponse.success(Map.of("loggedOut", true), clock);
    }

    @GetMapping("/me")
    ApiResponse<AuthUserResponse> me() {
        return ApiResponse.success(authService.currentUser(authenticatedUser.id()), clock);
    }

    @GetMapping("/sessions")
    ApiResponse<List<SessionResponse>> sessions() {
        return ApiResponse.success(
                authService.sessions(authenticatedUser.id(), authenticatedUser.sessionId()), clock);
    }

    @DeleteMapping("/sessions/{sessionId}")
    ApiResponse<Map<String, Boolean>> revokeSession(@PathVariable UUID sessionId) {
        authService.revokeSession(authenticatedUser.id(), sessionId);
        return ApiResponse.success(Map.of("revoked", true), clock);
    }

    @DeleteMapping("/sessions")
    ApiResponse<Map<String, Integer>> revokeAllSessions(HttpServletResponse servletResponse) {
        int revoked = authService.revokeAllSessions(authenticatedUser.id());
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, cookieService.clearRefreshCookie().toString());
        return ApiResponse.success(Map.of("revokedSessions", revoked), clock);
    }

    private void setRefreshCookieIfPresent(AuthService.AuthResult result, HttpServletResponse response) {
        if (result.refreshToken() != null) {
            response.addHeader(HttpHeaders.SET_COOKIE, cookieService.refreshCookie(result.refreshToken()).toString());
        }
    }
}
