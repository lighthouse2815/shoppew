package com.shoppew.product.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.api.PageResponse;
import com.shoppew.product.dto.AttributeValueInput;
import com.shoppew.product.dto.AttributeDefinitionResponse;
import com.shoppew.product.dto.OptionMetadataRequest;
import com.shoppew.product.dto.OptionValueRequest;
import com.shoppew.product.dto.OptionValueResponse;
import com.shoppew.product.dto.ProductAttributeResponse;
import com.shoppew.product.dto.ProductAttributesRequest;
import com.shoppew.product.dto.ProductDetailResponse;
import com.shoppew.product.dto.ProductImageResponse;
import com.shoppew.product.dto.ProductOptionRequest;
import com.shoppew.product.dto.ProductOptionResponse;
import com.shoppew.product.dto.ProductRequest;
import com.shoppew.product.dto.ProductSummaryResponse;
import com.shoppew.product.dto.VariantRequest;
import com.shoppew.product.dto.VariantResponse;
import com.shoppew.product.entity.ProductStatus;
import com.shoppew.product.service.ProductAttributeService;
import com.shoppew.product.service.ProductConfigurationService;
import com.shoppew.product.service.ProductMediaService;
import com.shoppew.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/v1/seller/shops/{shopId}/products")
public class SellerProductController {

    private final ProductService productService;
    private final ProductConfigurationService configurationService;
    private final ProductAttributeService attributeService;
    private final ProductMediaService mediaService;
    private final AuthenticatedUser authenticatedUser;
    private final Clock clock;

    public SellerProductController(
            ProductService productService,
            ProductConfigurationService configurationService,
            ProductAttributeService attributeService,
            ProductMediaService mediaService,
            AuthenticatedUser authenticatedUser,
            Clock clock) {
        this.productService = productService;
        this.configurationService = configurationService;
        this.attributeService = attributeService;
        this.mediaService = mediaService;
        this.authenticatedUser = authenticatedUser;
        this.clock = clock;
    }

    @GetMapping
    ApiResponse<PageResponse<ProductSummaryResponse>> list(
            @PathVariable UUID shopId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "24") @Min(1) @Max(100) int size) {
        return ApiResponse.success(productService.sellerList(authenticatedUser.id(), shopId, status, page, size), clock);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<ProductDetailResponse> create(
            @PathVariable UUID shopId,
            @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success(productService.create(authenticatedUser.id(), shopId, request), clock);
    }

    @GetMapping("/attribute-definitions")
    ApiResponse<List<AttributeDefinitionResponse>> attributeDefinitions(
            @PathVariable UUID shopId,
            @RequestParam(required = false) UUID categoryId) {
        return ApiResponse.success(
                attributeService.sellerDefinitions(authenticatedUser.id(), shopId, categoryId), clock);
    }

    @GetMapping("/{productId}")
    ApiResponse<ProductDetailResponse> detail(@PathVariable UUID shopId, @PathVariable UUID productId) {
        return ApiResponse.success(productService.sellerDetail(authenticatedUser.id(), shopId, productId), clock);
    }

    @PutMapping("/{productId}")
    ApiResponse<ProductDetailResponse> update(
            @PathVariable UUID shopId,
            @PathVariable UUID productId,
            @Valid @RequestBody ProductRequest request) {
        return ApiResponse.success(productService.update(authenticatedUser.id(), shopId, productId, request), clock);
    }

    @PostMapping("/{productId}/submit")
    ApiResponse<ProductDetailResponse> submit(@PathVariable UUID shopId, @PathVariable UUID productId) {
        return ApiResponse.success(productService.submit(authenticatedUser.id(), shopId, productId), clock);
    }

    @DeleteMapping("/{productId}")
    ApiResponse<ProductDetailResponse> archive(@PathVariable UUID shopId, @PathVariable UUID productId) {
        return ApiResponse.success(productService.archive(authenticatedUser.id(), shopId, productId), clock);
    }

    @PostMapping("/{productId}/options")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<ProductOptionResponse> createOption(
            @PathVariable UUID shopId,
            @PathVariable UUID productId,
            @Valid @RequestBody ProductOptionRequest request) {
        return ApiResponse.success(
                configurationService.createOption(authenticatedUser.id(), shopId, productId, request), clock);
    }

    @PutMapping("/{productId}/options/{optionId}")
    ApiResponse<ProductOptionResponse> updateOption(
            @PathVariable UUID shopId,
            @PathVariable UUID productId,
            @PathVariable UUID optionId,
            @Valid @RequestBody OptionMetadataRequest request) {
        return ApiResponse.success(
                configurationService.updateOption(authenticatedUser.id(), shopId, productId, optionId, request), clock);
    }

    @DeleteMapping("/{productId}/options/{optionId}")
    ApiResponse<Map<String, Boolean>> deleteOption(
            @PathVariable UUID shopId,
            @PathVariable UUID productId,
            @PathVariable UUID optionId) {
        configurationService.deleteOption(authenticatedUser.id(), shopId, productId, optionId);
        return ApiResponse.success(Map.of("deleted", true), clock);
    }

    @PostMapping("/{productId}/options/{optionId}/values")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<OptionValueResponse> addOptionValue(
            @PathVariable UUID shopId,
            @PathVariable UUID productId,
            @PathVariable UUID optionId,
            @Valid @RequestBody OptionValueRequest request) {
        return ApiResponse.success(configurationService.addOptionValue(
                authenticatedUser.id(), shopId, productId, optionId, request), clock);
    }

    @PutMapping("/{productId}/options/{optionId}/values/{valueId}")
    ApiResponse<OptionValueResponse> updateOptionValue(
            @PathVariable UUID shopId,
            @PathVariable UUID productId,
            @PathVariable UUID optionId,
            @PathVariable UUID valueId,
            @Valid @RequestBody OptionValueRequest request) {
        return ApiResponse.success(configurationService.updateOptionValue(
                authenticatedUser.id(), shopId, productId, optionId, valueId, request), clock);
    }

    @DeleteMapping("/{productId}/options/{optionId}/values/{valueId}")
    ApiResponse<Map<String, Boolean>> deleteOptionValue(
            @PathVariable UUID shopId,
            @PathVariable UUID productId,
            @PathVariable UUID optionId,
            @PathVariable UUID valueId) {
        configurationService.deleteOptionValue(authenticatedUser.id(), shopId, productId, optionId, valueId);
        return ApiResponse.success(Map.of("deleted", true), clock);
    }

    @PostMapping("/{productId}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<VariantResponse> createVariant(
            @PathVariable UUID shopId,
            @PathVariable UUID productId,
            @Valid @RequestBody VariantRequest request) {
        return ApiResponse.success(configurationService.createVariant(
                authenticatedUser.id(), shopId, productId, request), clock);
    }

    @PutMapping("/{productId}/variants/{variantId}")
    ApiResponse<VariantResponse> updateVariant(
            @PathVariable UUID shopId,
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @Valid @RequestBody VariantRequest request) {
        return ApiResponse.success(configurationService.updateVariant(
                authenticatedUser.id(), shopId, productId, variantId, request), clock);
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    ApiResponse<Map<String, Boolean>> archiveVariant(
            @PathVariable UUID shopId,
            @PathVariable UUID productId,
            @PathVariable UUID variantId) {
        configurationService.archiveVariant(authenticatedUser.id(), shopId, productId, variantId);
        return ApiResponse.success(Map.of("archived", true), clock);
    }

    @PutMapping("/{productId}/attributes")
    ApiResponse<List<ProductAttributeResponse>> setAttributes(
            @PathVariable UUID shopId,
            @PathVariable UUID productId,
            @Valid @RequestBody ProductAttributesRequest request) {
        return ApiResponse.success(attributeService.setProductValues(
                authenticatedUser.id(), shopId, productId, request), clock);
    }

    @PostMapping(value = "/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<ProductImageResponse> uploadImage(
            @PathVariable UUID shopId,
            @PathVariable UUID productId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) @Size(max = 255) String altText,
            @RequestParam(defaultValue = "0") @Min(0) int sortOrder,
            @RequestParam(defaultValue = "false") boolean primary) {
        return ApiResponse.success(mediaService.upload(
                authenticatedUser.id(), shopId, productId, file, altText, sortOrder, primary), clock);
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    ApiResponse<Map<String, Boolean>> deleteImage(
            @PathVariable UUID shopId,
            @PathVariable UUID productId,
            @PathVariable UUID imageId) {
        mediaService.delete(authenticatedUser.id(), shopId, productId, imageId);
        return ApiResponse.success(Map.of("deleted", true), clock);
    }
}
