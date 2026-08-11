# Wishlist, review, and notification flow

## Wishlist ownership and idempotency

Wishlist routes always derive the user from the access token. Adding an active public product creates one `(user_id, product_id)` row; replay returns the existing row rather than creating a duplicate. Listing reuses current public product summaries, so price, promotion, availability, and rating data are not frozen in the wishlist. Deletion is scoped by both user and product.

## Verified-purchase reviews

The client submits only an `orderItemId`, rating, and optional content. The server loads that immutable item through its owning order and authenticated customer, requires order status `COMPLETED`, and derives the product and shop from the purchase. One database row per order item prevents duplicate reviews across retries.

Rating must be 1–5 and content is limited to 5,000 characters. An owner may update the rating/content and attach up to five validated JPEG, PNG, or WebP images. Media bytes use the same S3-compatible storage boundary as product images; PostgreSQL stores durable object keys and ordered metadata.

Published reviews are visible on the public product route. A shop owner/member may reply only to reviews for that shop. Admin moderation explicitly changes a review to `PUBLISHED`, `HIDDEN`, or `REMOVED`. Creation, owner edits, and moderation recompute `review_count` and two-decimal `rating_average` for both product and shop in the same transaction; hidden/removed rows do not contribute.

## Notification persistence and delivery

An order status transition publishes a local domain event. The notification listener joins the transaction and creates an owned `ORDER` notification containing the stable order reference and new state, plus one `IN_APP` delivery record marked `DELIVERED`. A failed order transaction therefore cannot leave a misleading notification. Persisted chat messages create the same owned in-app record with type `CHAT` for every other conversation participant.

Customers can page their own timeline, read an unread count, mark one owned notification read, or mark all owned notifications read. Object access is scoped by user ID at the repository boundary. The delivery table remains separate from the user-facing notification so each `IN_APP`, `EMAIL`, or `PUSH` attempt retains an explicit status and provider reference/reason.

Welcome and order-confirmation events request SMTP delivery after the originating transaction commits. Local Docker sends real multipart UTF-8 mail to Mailpit. Order confirmation and later order-status events also request push delivery. The current backend intentionally uses `LocalPushNotificationSender`, which returns `SKIPPED` because no push credentials are configured. Android can receive FCM data and route product/order/notification deep links, but there is not yet an authenticated device-token registration API or a credentialed server-side FCM provider. Production push must not be described as delivered until both exist and a real device receives an end-to-end message.

## Verification

The Testcontainers integration test covers duplicate-safe wishlist behavior, rejection before purchase completion, all five order transition notifications, single/read-all state, review creation/editing, duplicate prevention, seller reply, image upload, public visibility, moderation, and product/shop aggregate changes. The SMTP notification regression verifies a multipart UTF-8 body. `scripts/smoke-catalog.ps1` repeats the customer/seller path against the rebuilt Docker backend and verifies its database effects directly.

Current runtime smoke `20260811101130` finished with `Status: PASS`. Its final database evidence was `PUBLISHED|5|1|5.00|1|5.00|9|9|1|1` for review/product/shop/notification/wishlist/image state and `2|8` for two delivered SMTP messages plus eight explicitly skipped push attempts. Mailpit independently showed the buyer's welcome and order-confirmation messages. These counts prove transparent adapter state; they do not close the production push gap.
