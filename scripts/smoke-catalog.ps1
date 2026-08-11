[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost:28080'
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$repoRoot = Split-Path -Parent $PSScriptRoot
$runId = Get-Date -Format 'yyyyMMddHHmmss'
$password = 'ShoppewSmoke2026!'
$adminEmail = "catalog-admin-$runId@example.test"
$sellerEmail = "catalog-seller-$runId@example.test"

function Invoke-ShoppewJson {
    param(
        [Parameter(Mandatory)] [string]$Method,
        [Parameter(Mandatory)] [string]$Path,
        [object]$Body,
        [string]$Token,
        [hashtable]$Headers
    )

    $parameters = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        Headers = @{ 'X-Request-Id' = "smoke-$runId" }
    }
    if ($Token) {
        $parameters.Headers.Authorization = "Bearer $Token"
    }
    if ($Headers) {
        foreach ($header in $Headers.GetEnumerator()) {
            $parameters.Headers[$header.Key] = $header.Value
        }
    }
    if ($null -ne $Body) {
        $parameters.ContentType = 'application/json'
        $parameters.Body = $Body | ConvertTo-Json -Depth 12 -Compress
    }
    return Invoke-RestMethod @parameters
}

function Assert-Value {
    param(
        [Parameter(Mandatory)] $Actual,
        [Parameter(Mandatory)] $Expected,
        [Parameter(Mandatory)] [string]$Label
    )
    if ($Actual -ne $Expected) {
        throw "$Label expected '$Expected' but received '$Actual'"
    }
}

function Get-SmokePngBytes {
    # A complete, decodable 1x1 PNG. Keeping smoke media valid catches the same
    # rendering path used by the storefront, Seller Center and Android client.
    return [Convert]::FromBase64String(
        'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII='
    )
}

function Add-ProductImage {
    param(
        [Parameter(Mandatory)] [string]$ShopId,
        [Parameter(Mandatory)] [string]$ProductId,
        [Parameter(Mandatory)] [string]$Token
    )

    $client = [System.Net.Http.HttpClient]::new()
    $content = [System.Net.Http.MultipartFormDataContent]::new()
    try {
        $client.DefaultRequestHeaders.Authorization =
            [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $Token)
        $client.DefaultRequestHeaders.Add('X-Request-Id', "smoke-$runId")

        [byte[]]$png = Get-SmokePngBytes
        $imageContent = [System.Net.Http.ByteArrayContent]::new($png)
        $imageContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::new('image/png')
        $content.Add($imageContent, 'file', 'shoppew-smoke.png')
        $content.Add([System.Net.Http.StringContent]::new('Shoppew catalog smoke image'), 'altText')
        $content.Add([System.Net.Http.StringContent]::new('true'), 'primary')

        $uri = "$BaseUrl/api/v1/seller/shops/$ShopId/products/$ProductId/images"
        $response = $client.PostAsync($uri, $content).GetAwaiter().GetResult()
        $responseBody = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) {
            throw "Image upload failed with HTTP $([int]$response.StatusCode): $responseBody"
        }
        return $responseBody | ConvertFrom-Json
    }
    finally {
        $content.Dispose()
        $client.Dispose()
    }
}

function Add-ReviewImage {
    param(
        [Parameter(Mandatory)] [string]$ReviewId,
        [Parameter(Mandatory)] [string]$Token
    )

    $client = [System.Net.Http.HttpClient]::new()
    $content = [System.Net.Http.MultipartFormDataContent]::new()
    try {
        $client.DefaultRequestHeaders.Authorization =
            [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $Token)
        $client.DefaultRequestHeaders.Add('X-Request-Id', "smoke-$runId")

        [byte[]]$png = Get-SmokePngBytes
        $imageContent = [System.Net.Http.ByteArrayContent]::new($png)
        $imageContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::new('image/png')
        $content.Add($imageContent, 'file', 'shoppew-review-smoke.png')

        $uri = "$BaseUrl/api/v1/reviews/$ReviewId/images"
        $response = $client.PostAsync($uri, $content).GetAwaiter().GetResult()
        $responseBody = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) {
            throw "Review image upload failed with HTTP $([int]$response.StatusCode): $responseBody"
        }
        return $responseBody | ConvertFrom-Json
    }
    finally {
        $content.Dispose()
        $client.Dispose()
    }
}

Push-Location $repoRoot
try {
    $health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health/readiness"
    Assert-Value $health.status 'UP' 'Backend readiness'

    $adminRegistration = Invoke-ShoppewJson POST '/api/v1/auth/register' @{
        email = $adminEmail
        password = $password
        displayName = 'Catalog Smoke Admin'
        deviceName = 'catalog-smoke'
    }
    Assert-Value $adminRegistration.success $true 'Admin registration'

    $adminSql = @"
INSERT INTO user_roles (user_id, role)
SELECT id, 'ADMIN' FROM app_users WHERE email = '$adminEmail'
ON CONFLICT DO NOTHING;
"@
    $adminSql | docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U shoppew -d shoppew | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw 'Could not grant the development smoke account the ADMIN role'
    }

    $adminLogin = Invoke-ShoppewJson POST '/api/v1/auth/login' @{
        email = $adminEmail
        password = $password
        deviceName = 'catalog-smoke-admin'
    }
    $adminToken = $adminLogin.data.accessToken

    $rootCategory = Invoke-ShoppewJson POST '/api/v1/admin/categories' @{
        name = 'Smoke Fashion'
        slug = "smoke-fashion-$runId"
        description = 'Runtime catalog verification root'
        sortOrder = 900
    } $adminToken
    $category = Invoke-ShoppewJson POST '/api/v1/admin/categories' @{
        name = 'Smoke T-shirts'
        slug = "smoke-tshirts-$runId"
        parentId = $rootCategory.data.id
        description = 'Runtime catalog verification leaf'
        sortOrder = 1
    } $adminToken
    $brand = Invoke-ShoppewJson POST '/api/v1/admin/brands' @{
        name = "Smoke Studio $runId"
        slug = "smoke-studio-$runId"
    } $adminToken
    $attribute = Invoke-ShoppewJson POST '/api/v1/admin/products/attributes' @{
        categoryId = $category.data.id
        name = "Smoke material $runId"
        valueType = 'TEXT'
        required = $true
        sortOrder = 1
    } $adminToken

    $sellerRegistration = Invoke-ShoppewJson POST '/api/v1/auth/register' @{
        email = $sellerEmail
        password = $password
        displayName = 'Catalog Smoke Seller'
        deviceName = 'catalog-smoke-seller'
    }
    $sellerToken = $sellerRegistration.data.accessToken
    $shop = Invoke-ShoppewJson POST '/api/v1/seller/shops' @{
        name = "Smoke Market $runId"
        slug = "smoke-market-$runId"
        description = 'Runtime catalog verification shop'
    } $sellerToken
    $shopId = $shop.data.id
    $activatedShop = Invoke-ShoppewJson PATCH "/api/v1/admin/shops/$shopId/status" @{
        status = 'ACTIVE'
    } $adminToken
    Assert-Value $activatedShop.data.status 'ACTIVE' 'Shop moderation'

    $product = Invoke-ShoppewJson POST "/api/v1/seller/shops/$shopId/products" @{
        categoryId = $category.data.id
        brandId = $brand.data.id
        name = "Shoppew Runtime Tee $runId"
        slug = "shoppew-runtime-tee-$runId"
        shortDescription = 'A product created by the runtime smoke test'
        description = 'This product verifies catalog persistence, moderation and media storage.'
    } $sellerToken
    $productId = $product.data.id

    $color = Invoke-ShoppewJson POST "/api/v1/seller/shops/$shopId/products/$productId/options" @{
        name = 'Color'
        sortOrder = 1
        values = @(@{ value = 'Ink'; sortOrder = 1 })
    } $sellerToken
    $size = Invoke-ShoppewJson POST "/api/v1/seller/shops/$shopId/products/$productId/options" @{
        name = 'Size'
        sortOrder = 2
        values = @(@{ value = 'M'; sortOrder = 1 })
    } $sellerToken
    $variant = Invoke-ShoppewJson POST "/api/v1/seller/shops/$shopId/products/$productId/variants" @{
        sku = "RUNTIME-$runId"
        name = 'Ink / M'
        price = 219000
        compareAtPrice = 259000
        currency = 'VND'
        weightGrams = 240
        optionValueIds = @($color.data.values[0].id, $size.data.values[0].id)
    } $sellerToken
    Assert-Value $variant.data.price 219000 'Variant price'

    $null = Invoke-ShoppewJson PUT "/api/v1/seller/shops/$shopId/products/$productId/attributes" @{
        values = @(@{ attributeId = $attribute.data.id; value = '100% cotton' })
    } $sellerToken
    $image = Add-ProductImage $shopId $productId $sellerToken
    Assert-Value $image.data.primary $true 'Primary image'

    $storedImage = Invoke-WebRequest -Uri $image.data.url -UseBasicParsing
    Assert-Value $storedImage.StatusCode 200 'MinIO public object'
    Assert-Value $storedImage.RawContentLength 68 'MinIO object size'

    $submitted = Invoke-ShoppewJson POST "/api/v1/seller/shops/$shopId/products/$productId/submit" $null $sellerToken
    Assert-Value $submitted.data.status 'PENDING_REVIEW' 'Product submission'
    $approved = Invoke-ShoppewJson POST "/api/v1/admin/products/$productId/approve" $null $adminToken
    Assert-Value $approved.data.status 'ACTIVE' 'Product approval'

    $variantId = $variant.data.id
    $stocked = Invoke-ShoppewJson POST "/api/v1/seller/shops/$shopId/inventory/$variantId/adjustments" @{
        mode = 'INCREASE'
        quantity = 25
        lowStockThreshold = 5
        note = 'Runtime smoke initial stock'
    } $sellerToken
    Assert-Value $stocked.data.availableQuantity 25 'Seller inventory adjustment'

    $buyerEmail = "catalog-buyer-$runId@example.test"
    $buyerRegistration = Invoke-ShoppewJson POST '/api/v1/auth/register' @{
        email = $buyerEmail
        password = $password
        displayName = 'Catalog Smoke Buyer'
        deviceName = 'catalog-smoke-buyer'
    }
    $buyerToken = $buyerRegistration.data.accessToken
    $buyerAddress = Invoke-ShoppewJson POST '/api/v1/users/me/addresses' @{
        label = 'Runtime home'
        recipientName = 'Catalog Smoke Buyer'
        phone = '0901234567'
        countryCode = 'VN'
        province = 'Ho Chi Minh City'
        district = 'District 1'
        ward = 'Ben Nghe'
        addressLine = '1 Shoppew Runtime Street'
        postalCode = '700000'
        defaultAddress = $true
    } $buyerToken
    $buyerId = $buyerRegistration.data.user.id

    $wishlistFirst = Invoke-ShoppewJson POST "/api/v1/wishlist/products/$productId" $null $buyerToken
    $wishlistReplay = Invoke-ShoppewJson POST "/api/v1/wishlist/products/$productId" $null $buyerToken
    Assert-Value $wishlistReplay.data.id $wishlistFirst.data.id 'Wishlist idempotent add'
    $wishlist = Invoke-ShoppewJson GET '/api/v1/wishlist' $null $buyerToken
    Assert-Value $wishlist.data.Count 1 'Wishlist unique product'
    $null = Invoke-ShoppewJson DELETE "/api/v1/wishlist/products/$productId" $null $buyerToken
    $emptyWishlist = Invoke-ShoppewJson GET '/api/v1/wishlist' $null $buyerToken
    Assert-Value $emptyWishlist.data.Count 0 'Wishlist delete'
    $wishlistFinal = Invoke-ShoppewJson POST "/api/v1/wishlist/products/$productId" $null $buyerToken

    $cart = Invoke-ShoppewJson POST '/api/v1/cart/items' @{
        variantId = $variantId
        quantity = 2
    } $buyerToken
    Assert-Value $cart.data.shops.Count 1 'Cart shop grouping'
    Assert-Value $cart.data.itemCount 2 'Cart item count'
    Assert-Value $cart.data.shops[0].items[0].unitPrice 219000 'Server cart price'
    Assert-Value $cart.data.shops[0].items[0].eligible $true 'Initial cart eligibility'

    $null = Invoke-ShoppewJson POST "/api/v1/seller/shops/$shopId/inventory/$variantId/adjustments" @{
        mode = 'SET'
        quantity = 1
        lowStockThreshold = 5
        note = 'Runtime smoke cart revalidation'
    } $sellerToken
    $revalidatedCart = Invoke-ShoppewJson GET '/api/v1/cart' $null $buyerToken
    Assert-Value $revalidatedCart.data.shops[0].items[0].eligible $false 'Revalidated cart eligibility'
    if ($revalidatedCart.data.shops[0].items[0].issues -notcontains 'INSUFFICIENT_STOCK') {
        throw 'Cart did not expose INSUFFICIENT_STOCK after authoritative stock changed'
    }
    $restocked = Invoke-ShoppewJson POST "/api/v1/seller/shops/$shopId/inventory/$variantId/adjustments" @{
        mode = 'SET'
        quantity = 25
        lowStockThreshold = 5
        note = 'Runtime smoke restore stock'
    } $sellerToken
    Assert-Value $restocked.data.availableQuantity 25 'Restored stock'

    $promotionStartsAt = (Get-Date).ToUniversalTime().AddMinutes(-1).ToString('o')
    $promotionEndsAt = (Get-Date).ToUniversalTime().AddHours(2).ToString('o')
    $promotion = Invoke-ShoppewJson POST "/api/v1/seller/shops/$shopId/promotions" @{
        name = "Runtime flash price $runId"
        promotionType = 'FLASH_SALE'
        discountType = 'PERCENTAGE'
        discountValue = 10
        maxDiscount = 30000
        startsAt = $promotionStartsAt
        endsAt = $promotionEndsAt
        targets = @(@{
            productId = $productId
            variantId = $variantId
            promotionalPrice = 200000
            quantityLimit = 10
        })
    } $sellerToken
    $promotion = Invoke-ShoppewJson POST "/api/v1/seller/shops/$shopId/promotions/$($promotion.data.id)/activate" $null $sellerToken
    Assert-Value $promotion.data.status 'ACTIVE' 'Promotion activation'

    $shopVoucherCode = "SHOP$runId"
    $shopVoucher = Invoke-ShoppewJson POST "/api/v1/seller/shops/$shopId/vouchers" @{
        code = $shopVoucherCode
        name = "Runtime product voucher $runId"
        voucherType = 'PRODUCT'
        discountType = 'FIXED'
        discountValue = 10000
        minimumSpend = 100000
        currency = 'VND'
        startsAt = $promotionStartsAt
        endsAt = $promotionEndsAt
        totalQuantity = 10
        perUserLimit = 1
        productIds = @($productId)
        paymentProviders = @('COD')
    } $sellerToken
    $shopVoucher = Invoke-ShoppewJson POST "/api/v1/seller/shops/$shopId/vouchers/$($shopVoucher.data.id)/activate" $null $sellerToken
    Assert-Value $shopVoucher.data.status 'ACTIVE' 'Seller voucher activation'

    $platformVoucherCode = "SHIP$runId"
    $platformVoucher = Invoke-ShoppewJson POST '/api/v1/admin/vouchers' @{
        code = $platformVoucherCode
        name = "Runtime shipping voucher $runId"
        voucherType = 'SHIPPING'
        discountType = 'FIXED'
        discountValue = 5000
        minimumSpend = 0
        currency = 'VND'
        startsAt = $promotionStartsAt
        endsAt = $promotionEndsAt
        totalQuantity = 10
        perUserLimit = 1
    } $adminToken
    $platformVoucher = Invoke-ShoppewJson POST "/api/v1/admin/vouchers/$($platformVoucher.data.id)/activate" $null $adminToken
    Assert-Value $platformVoucher.data.status 'ACTIVE' 'Platform voucher activation'

    $promotedCart = Invoke-ShoppewJson GET '/api/v1/cart' $null $buyerToken
    Assert-Value $promotedCart.data.shops[0].items[0].unitPrice 200000 'Promoted cart price'
    Assert-Value $promotedCart.data.shops[0].items[0].originalUnitPrice 219000 'Original cart price'
    Assert-Value $promotedCart.data.shops[0].items[0].promotionId $promotion.data.id 'Cart promotion metadata'

    $cartItemId = $cart.data.shops[0].items[0].id
    $checkoutRequest = @{
        cartItemIds = @($cartItemId)
        addressId = $buyerAddress.data.id
        paymentProvider = 'COD'
        shippingMethodCode = 'MOCK_STANDARD'
        customerNote = 'Runtime checkout verification'
        voucherCodes = @($shopVoucherCode, $platformVoucherCode)
    }
    $preview = Invoke-ShoppewJson POST '/api/v1/checkout/preview' $checkoutRequest $buyerToken
    Assert-Value $preview.data.shops.Count 1 'Checkout shop grouping'
    Assert-Value $preview.data.itemsSubtotal 400000 'Checkout authoritative promoted subtotal'
    Assert-Value $preview.data.shippingTotal 22000 'Checkout shipping quote'
    Assert-Value $preview.data.discountTotal 15000 'Checkout voucher discount'
    Assert-Value $preview.data.grandTotal 407000 'Checkout grand total'
    Assert-Value $preview.data.appliedVouchers.Count 2 'Checkout applied vouchers'

    $checkoutKey = "smoke-checkout-$runId"
    $checkout = Invoke-ShoppewJson POST '/api/v1/checkout' $checkoutRequest $buyerToken @{
        'Idempotency-Key' = $checkoutKey
    }
    Assert-Value $checkout.data.status 'CONFIRMED' 'COD checkout status'
    Assert-Value $checkout.data.orders.Count 1 'Checkout seller order split'
    Assert-Value $checkout.data.payment.status 'PENDING' 'COD payment status'
    $orderId = $checkout.data.orders[0].id
    Assert-Value $checkout.data.orders[0].status 'CONFIRMED' 'COD order initial state'

    $checkoutReplay = Invoke-ShoppewJson POST '/api/v1/checkout' $checkoutRequest $buyerToken @{
        'Idempotency-Key' = $checkoutKey
    }
    Assert-Value $checkoutReplay.data.id $checkout.data.id 'Checkout idempotent replay'

    $processed = Invoke-ShoppewJson POST "/api/v1/seller/shops/$shopId/orders/$orderId/process" @{
        reason = 'Runtime seller processing'
    } $sellerToken
    Assert-Value $processed.data.status 'PROCESSING' 'Seller process transition'
    $readyOrder = Invoke-ShoppewJson POST "/api/v1/seller/shops/$shopId/orders/$orderId/ready-to-ship" @{
        reason = 'Runtime package ready'
    } $sellerToken
    Assert-Value $readyOrder.data.status 'READY_TO_SHIP' 'Seller ready transition'
    $shippedOrder = Invoke-ShoppewJson POST "/api/v1/seller/shops/$shopId/orders/$orderId/ship" @{
        trackingNumber = "SMOKE-$runId"
        location = 'Shoppew runtime hub'
    } $sellerToken
    Assert-Value $shippedOrder.data.status 'SHIPPED' 'Seller ship transition'
    Assert-Value $shippedOrder.data.shipment.trackingNumber "SMOKE-$runId" 'Shipment tracking number'
    $deliveredOrder = Invoke-ShoppewJson POST "/api/v1/seller/shops/$shopId/orders/$orderId/deliver" @{
        location = 'Runtime recipient address'
    } $sellerToken
    Assert-Value $deliveredOrder.data.status 'DELIVERED' 'Seller deliver transition'
    $completedOrder = Invoke-ShoppewJson POST "/api/v1/orders/$orderId/complete" $null $buyerToken
    Assert-Value $completedOrder.data.status 'COMPLETED' 'Customer complete transition'
    Assert-Value $completedOrder.data.history.Count 6 'Order history length'
    Assert-Value $completedOrder.data.items[0].productName "Shoppew Runtime Tee $runId" 'Order product snapshot'
    Assert-Value $completedOrder.data.items[0].unitPrice 200000 'Order promoted price snapshot'

    $notifications = Invoke-ShoppewJson GET '/api/v1/notifications' $null $buyerToken
    Assert-Value $notifications.data.totalElements 7 'Order notification count'
    $unreadNotifications = Invoke-ShoppewJson GET '/api/v1/notifications/unread-count' $null $buyerToken
    Assert-Value $unreadNotifications.data.count 7 'Unread notification count'
    $notificationId = $notifications.data.content[0].id
    $readNotification = Invoke-ShoppewJson POST "/api/v1/notifications/$notificationId/read" $null $buyerToken
    Assert-Value $readNotification.data.read $true 'Notification read state'
    $null = Invoke-ShoppewJson POST '/api/v1/notifications/read-all' $null $buyerToken
    $unreadAfterReadAll = Invoke-ShoppewJson GET '/api/v1/notifications/unread-count' $null $buyerToken
    Assert-Value $unreadAfterReadAll.data.count 0 'Notification read-all'

    $orderItemId = $completedOrder.data.items[0].id
    $review = Invoke-ShoppewJson POST '/api/v1/reviews' @{
        orderItemId = $orderItemId
        rating = 5
        content = 'Verified runtime purchase review'
    } $buyerToken
    Assert-Value $review.data.status 'PUBLISHED' 'Verified review publication'
    Assert-Value $review.data.rating 5 'Verified review rating'
    $reviewImage = Add-ReviewImage -ReviewId $review.data.id -Token $buyerToken
    Assert-Value $reviewImage.data.images.Count 1 'Review image upload'
    $sellerReply = Invoke-ShoppewJson PUT "/api/v1/seller/shops/$shopId/reviews/$($review.data.id)/reply" @{
        reply = 'Thank you for the verified runtime review'
    } $sellerToken
    Assert-Value $sellerReply.data.sellerReply 'Thank you for the verified runtime review' 'Seller review reply'
    $publicReviews = Invoke-ShoppewJson GET "/api/v1/public/products/$productId/reviews"
    Assert-Value $publicReviews.data.totalElements 1 'Public verified reviews'

    $financeBeforeRefund = Invoke-ShoppewJson GET "/api/v1/seller/shops/$shopId/finance/balance" $null $sellerToken
    Assert-Value $financeBeforeRefund.data.pendingAmount 0 'Seller pending balance after completion'
    Assert-Value $financeBeforeRefund.data.availableAmount 370500 'Seller available balance after completion'
    $financeTransactions = Invoke-ShoppewJson GET "/api/v1/seller/shops/$shopId/finance/transactions" $null $sellerToken
    Assert-Value $financeTransactions.data.totalElements 4 'Seller settlement ledger entries'

    $refundRequest = Invoke-ShoppewJson POST '/api/v1/refunds' @{
        orderId = $orderId
        reason = 'NOT_AS_DESCRIBED'
        description = 'Runtime persisted refund evidence'
        items = @(@{
            orderItemId = $orderItemId
            quantity = 1
        })
    } $buyerToken
    Assert-Value $refundRequest.data.status 'REQUESTED' 'Refund requested state'
    Assert-Value $refundRequest.data.requestedAmount 192500 'Server-prorated refund amount'
    $refundRequest = Invoke-ShoppewJson POST "/api/v1/seller/shops/$shopId/refunds/$($refundRequest.data.id)/review" @{
        note = 'Runtime seller review evidence'
    } $sellerToken
    Assert-Value $refundRequest.data.status 'UNDER_REVIEW' 'Seller refund review state'

    $dispute = Invoke-ShoppewJson POST '/api/v1/disputes' @{
        orderId = $orderId
        refundRequestId = $refundRequest.data.id
        reason = 'REFUND_REVIEW'
        description = 'Runtime dispute foundation evidence'
    } $buyerToken
    Assert-Value $dispute.data.status 'OPEN' 'Customer dispute open state'
    $dispute = Invoke-ShoppewJson POST "/api/v1/seller/shops/$shopId/disputes/$($dispute.data.id)/messages" @{
        content = 'Runtime seller dispute response'
        attachments = @()
    } $sellerToken
    Assert-Value $dispute.data.messages.Count 1 'Seller dispute message persistence'

    $refundRequest = Invoke-ShoppewJson POST "/api/v1/admin/refunds/$($refundRequest.data.id)/approve" @{
        approvedAmount = 192500
        note = 'Runtime admin approval evidence'
    } $adminToken
    Assert-Value $refundRequest.data.status 'APPROVED' 'Admin refund approval state'
    $refundKey = "smoke-refund-$runId"
    $refundRequest = Invoke-ShoppewJson POST "/api/v1/admin/refunds/$($refundRequest.data.id)/process" $null $adminToken @{
        'Idempotency-Key' = $refundKey
    }
    Assert-Value $refundRequest.data.status 'REFUNDED' 'Refund terminal state'
    Assert-Value $refundRequest.data.refund.status 'SUCCEEDED' 'Persisted refund status'
    $refundReplay = Invoke-ShoppewJson POST "/api/v1/admin/refunds/$($refundRequest.data.id)/process" $null $adminToken @{
        'Idempotency-Key' = $refundKey
    }
    Assert-Value $refundReplay.data.refund.id $refundRequest.data.refund.id 'Refund idempotent replay'

    $dispute = Invoke-ShoppewJson PUT "/api/v1/admin/disputes/$($dispute.data.id)" @{
        status = 'RESOLVED'
        resolution = 'Runtime refund completed'
    } $adminToken
    Assert-Value $dispute.data.status 'RESOLVED' 'Admin dispute resolution'
    $financeAfterRefund = Invoke-ShoppewJson GET "/api/v1/seller/shops/$shopId/finance/balance" $null $sellerToken
    Assert-Value $financeAfterRefund.data.availableAmount 175500 'Seller available balance after refund'
    $sellerAnalytics = Invoke-ShoppewJson GET "/api/v1/seller/shops/$shopId/analytics" $null $sellerToken
    Assert-Value $sellerAnalytics.data.revenue 175500 'Seller real revenue analytics'
    Assert-Value $sellerAnalytics.data.completedOrders 1 'Seller real order analytics'
    $adminAnalytics = Invoke-ShoppewJson GET '/api/v1/admin/analytics' $null $adminToken
    if ($adminAnalytics.data.refundVolume -lt 192500) {
        throw "Admin refund analytics expected at least '192500' but received '$($adminAnalytics.data.refundVolume)'"
    }
    $auditLogs = Invoke-ShoppewJson GET '/api/v1/admin/audit-logs' $null $adminToken
    if ($auditLogs.data.totalElements -lt 3) {
        throw 'Admin audit timeline did not include the runtime critical operations'
    }

    $publicProduct = Invoke-ShoppewJson GET "/api/v1/public/products/$($product.data.slug)"
    Assert-Value $publicProduct.data.variants.Count 1 'Public variants'
    Assert-Value $publicProduct.data.variants[0].price 200000 'Public promoted price'
    Assert-Value $publicProduct.data.variants[0].originalPrice 219000 'Public original price'
    Assert-Value $publicProduct.data.images[0].url $image.data.url 'Public image URL'
    Assert-Value $publicProduct.data.attributes[0].value '100% cotton' 'Public attribute'

    $databaseEvidence = docker compose exec -T postgres psql -U shoppew -d shoppew -Atc "SELECT p.status || '|' || count(pi.id) FROM products p JOIN product_images pi ON pi.product_id = p.id WHERE p.id = '$productId' GROUP BY p.status;"
    Assert-Value $databaseEvidence 'ACTIVE|1' 'PostgreSQL product evidence'
    $objectKey = docker compose exec -T postgres psql -U shoppew -d shoppew -Atc "SELECT object_key FROM product_images WHERE product_id = '$productId';"
    if ([string]::IsNullOrWhiteSpace($objectKey)) {
        throw 'PostgreSQL did not retain the MinIO object key'
    }
    $commerceEvidence = docker compose exec -T postgres psql -U shoppew -d shoppew -Atc "SELECT o.status || '|' || p.status || '|' || i.available_quantity || '|' || i.reserved_quantity || '|' || i.sold_quantity FROM orders o JOIN checkout_groups cg ON cg.id = o.checkout_group_id JOIN payments p ON p.checkout_group_id = cg.id JOIN inventories i ON i.variant_id = '$variantId' WHERE o.id = '$orderId';"
    Assert-Value $commerceEvidence 'PARTIALLY_REFUNDED|PARTIALLY_REFUNDED|23|0|2' 'PostgreSQL commerce evidence'
    $discountEvidence = docker compose exec -T postgres psql -U shoppew -d shoppew -Atc "SELECT pp.sold_quantity || '|' || (SELECT count(*) FROM promotion_usages pu WHERE pu.checkout_group_id = '$($checkout.data.id)' AND pu.status = 'CONSUMED') || '|' || (SELECT count(*) FROM voucher_usages vu WHERE vu.checkout_group_id = '$($checkout.data.id)' AND vu.status = 'CONSUMED') || '|' || (SELECT sum(v.used_quantity) FROM vouchers v WHERE v.id IN ('$($shopVoucher.data.id)', '$($platformVoucher.data.id)')) FROM promotion_products pp WHERE pp.promotion_id = '$($promotion.data.id)';"
    Assert-Value $discountEvidence '2|1|2|2' 'PostgreSQL discount evidence'
    $engagementEvidence = docker compose exec -T postgres psql -U shoppew -d shoppew -Atc "SELECT r.status || '|' || r.rating || '|' || p.review_count || '|' || p.rating_average || '|' || s.review_count || '|' || s.rating_average || '|' || (SELECT count(*) FROM notifications n WHERE n.user_id = '$buyerId') || '|' || (SELECT count(*) FROM notification_deliveries d JOIN notifications n ON n.id = d.notification_id WHERE n.user_id = '$buyerId' AND d.channel = 'IN_APP' AND d.status = 'DELIVERED') || '|' || (SELECT count(*) FROM wishlists w WHERE w.user_id = '$buyerId') || '|' || (SELECT count(*) FROM review_images ri WHERE ri.review_id = r.id) FROM reviews r JOIN products p ON p.id = r.product_id JOIN shops s ON s.id = r.shop_id WHERE r.id = '$($review.data.id)';"
    Assert-Value $engagementEvidence 'PUBLISHED|5|1|5.00|1|5.00|9|9|1|1' 'PostgreSQL engagement evidence'
    $channelEvidence = docker compose exec -T postgres psql -U shoppew -d shoppew -Atc "SELECT count(*) FILTER (WHERE d.channel = 'EMAIL' AND d.status = 'DELIVERED') || '|' || count(*) FILTER (WHERE d.channel = 'PUSH' AND d.status = 'SKIPPED') FROM notification_deliveries d JOIN notifications n ON n.id = d.notification_id WHERE n.user_id = '$buyerId';"
    Assert-Value $channelEvidence '2|8' 'Notification channel adapter evidence'
    $operationsEvidence = docker compose exec -T postgres psql -U shoppew -d shoppew -Atc "SELECT rr.status || '|' || rf.status || '|' || o.status || '|' || p.status || '|' || d.status || '|' || sb.available_amount || '|' || (SELECT count(*) FROM seller_transactions st WHERE st.order_id = o.id) || '|' || (SELECT count(*) FROM audit_logs al WHERE al.action IN ('REFUND_APPROVED','REFUND_PROCESSED','DISPUTE_STATUS_CHANGED') AND al.actor_id = '$($adminRegistration.data.user.id)') FROM refund_requests rr JOIN refunds rf ON rf.refund_request_id = rr.id JOIN orders o ON o.id = rr.order_id JOIN payments p ON p.checkout_group_id = o.checkout_group_id JOIN disputes d ON d.refund_request_id = rr.id JOIN seller_balances sb ON sb.shop_id = rr.shop_id WHERE rr.id = '$($refundRequest.data.id)';"
    Assert-Value $operationsEvidence 'REFUNDED|SUCCEEDED|PARTIALLY_REFUNDED|PARTIALLY_REFUNDED|RESOLVED|175500.00|5|3' 'PostgreSQL operations evidence'

    Write-Host 'Status: PASS'
    Write-Host "Run: $runId"
    Write-Host "Product: $($publicProduct.data.slug)"
    Write-Host "Object: $objectKey"
    Write-Host "Public URL: $($image.data.url)"
    Write-Host "Database: $databaseEvidence"
    Write-Host "Inventory: $($restocked.data.availableQuantity) available"
    Write-Host "Cart: grouped by 1 shop and revalidated insufficient stock"
    Write-Host "Checkout: $($checkout.data.checkoutNumber), idempotent COD, total $($checkout.data.grandTotal) VND"
    Write-Host "Order: $($completedOrder.data.orderNumber), explicit state machine completed"
    Write-Host "Commerce database: $commerceEvidence"
    Write-Host "Discount database: $discountEvidence"
    Write-Host "Engagement database: $engagementEvidence"
    Write-Host "Notification channels: $channelEvidence"
    Write-Host "Operations database: $operationsEvidence"
}
finally {
    Pop-Location
}
