package com.shoppew.review.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.api.PageResponse;
import com.shoppew.review.dto.ReviewRequest;
import com.shoppew.review.dto.ReviewResponse;
import com.shoppew.review.dto.ReviewUpdateRequest;
import com.shoppew.review.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {
    private final ReviewService service; private final AuthenticatedUser user; private final Clock clock;
    public ReviewController(ReviewService service, AuthenticatedUser user, Clock clock) { this.service = service; this.user = user; this.clock = clock; }
    @GetMapping("/me") ApiResponse<PageResponse<ReviewResponse>> mine(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.mine(user.id(), page, size), clock);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<ReviewResponse> create(@Valid @RequestBody ReviewRequest request) { return ApiResponse.success(service.create(user.id(), request), clock); }
    @PutMapping("/{reviewId}") ApiResponse<ReviewResponse> update(@PathVariable UUID reviewId, @Valid @RequestBody ReviewUpdateRequest request) { return ApiResponse.success(service.update(user.id(), reviewId, request), clock); }
    @PostMapping(path = "/{reviewId}/images", consumes = "multipart/form-data") @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<ReviewResponse> upload(@PathVariable UUID reviewId, @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "0") int sortOrder) { return ApiResponse.success(service.upload(user.id(), reviewId, file, sortOrder), clock); }
    @DeleteMapping("/{reviewId}/images/{imageId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteImage(@PathVariable UUID reviewId, @PathVariable UUID imageId) { service.deleteImage(user.id(), reviewId, imageId); }
}
