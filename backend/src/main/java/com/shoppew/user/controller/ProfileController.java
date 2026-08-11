package com.shoppew.user.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.user.dto.ProfileResponse;
import com.shoppew.user.dto.UpdateProfileRequest;
import com.shoppew.user.service.ProfileService;
import jakarta.validation.Valid;
import java.time.Clock;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final AuthenticatedUser authenticatedUser;
    private final Clock clock;

    public ProfileController(ProfileService profileService, AuthenticatedUser authenticatedUser, Clock clock) {
        this.profileService = profileService;
        this.authenticatedUser = authenticatedUser;
        this.clock = clock;
    }

    @GetMapping
    ApiResponse<ProfileResponse> get() {
        return ApiResponse.success(profileService.get(authenticatedUser.id()), clock);
    }

    @PutMapping
    ApiResponse<ProfileResponse> update(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(profileService.update(authenticatedUser.id(), request), clock);
    }
}
