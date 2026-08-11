package com.shoppew.address.controller;

import com.shoppew.address.dto.AddressRequest;
import com.shoppew.address.dto.AddressResponse;
import com.shoppew.address.service.AddressService;
import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import jakarta.validation.Valid;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/addresses")
public class AddressController {

    private final AddressService addressService;
    private final AuthenticatedUser authenticatedUser;
    private final Clock clock;

    public AddressController(AddressService addressService, AuthenticatedUser authenticatedUser, Clock clock) {
        this.addressService = addressService;
        this.authenticatedUser = authenticatedUser;
        this.clock = clock;
    }

    @GetMapping
    ApiResponse<List<AddressResponse>> list() {
        return ApiResponse.success(addressService.list(authenticatedUser.id()), clock);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<AddressResponse> create(@Valid @RequestBody AddressRequest request) {
        return ApiResponse.success(addressService.create(authenticatedUser.id(), request), clock);
    }

    @PutMapping("/{addressId}")
    ApiResponse<AddressResponse> update(@PathVariable UUID addressId, @Valid @RequestBody AddressRequest request) {
        return ApiResponse.success(addressService.update(authenticatedUser.id(), addressId, request), clock);
    }

    @PatchMapping("/{addressId}/default")
    ApiResponse<AddressResponse> setDefault(@PathVariable UUID addressId) {
        return ApiResponse.success(addressService.setDefault(authenticatedUser.id(), addressId), clock);
    }

    @DeleteMapping("/{addressId}")
    ApiResponse<Map<String, Boolean>> delete(@PathVariable UUID addressId) {
        addressService.delete(authenticatedUser.id(), addressId);
        return ApiResponse.success(Map.of("deleted", true), clock);
    }
}
