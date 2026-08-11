package com.shoppew.product.service;

import com.shoppew.media.StorageService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class ProductImageStorageListener {

    private final StorageService storageService;

    ProductImageStorageListener(StorageService storageService) {
        this.storageService = storageService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void deleteObject(ProductImageDeletedEvent event) {
        storageService.delete(event.objectKey());
    }
}
