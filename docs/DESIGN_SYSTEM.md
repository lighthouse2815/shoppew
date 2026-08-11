# shoppew design system

## Product character

shoppew is a fast, modern Vietnamese marketplace: commercially dense, calm enough to trust, and energetic at moments of action. The identity must remain original and must not reproduce another marketplace's logo, trade dress, layout, graphics, or dominant orange language.

The lowercase wordmark `shoppew` is direct and friendly. The future symbol is a continuous route that turns from a parcel edge into a subtle `w`; it must remain legible at favicon size and work in one color. Until the symbol is finalized, use the wordmark rather than a generic cart or bag mark.

## Color tokens

| Token | Value | Use |
| --- | --- | --- |
| `brand-950` | `#102F2E` | Primary ink, dark navigation |
| `brand-800` | `#174B49` | Strong brand surfaces |
| `brand-600` | `#087E76` | Primary action, active state |
| `brand-500` | `#0A9489` | Interactive emphasis |
| `brand-100` | `#DDF3EF` | Selected and informational tint |
| `spark-400` | `#D8ED4B` | Small energetic accent, never body text |
| `violet-500` | `#6258D6` | Campaign/supporting accent |
| `coral-500` | `#D9604C` | Urgency and warm promotion accent |
| `canvas` | `#F5F7F4` | Application background |
| `surface` | `#FFFFFF` | Primary content surface |
| `ink` | `#18211F` | Default text |
| `muted` | `#65716D` | Secondary text |
| `line` | `#DCE3DF` | Dividers and controls |
| `success` | `#18794E` | Success |
| `warning` | `#A15C00` | Warning |
| `danger` | `#B42318` | Error/destructive |
| `info` | `#2457C5` | Informational |

Use semantic tokens in components instead of raw hex values. `spark-400` needs a dark foreground. Do not communicate status through color alone. Gradients are reserved for rare campaign art and never substitute for hierarchy.

## Typography

- Primary family: Be Vietnam Pro, shipped with each client or installed as a package; fallback to `system-ui`, `Segoe UI`, sans-serif.
- Android: use the platform-safe Vietnamese-capable family while matching the same scale and weights.
- Body: 14–16 px web / 14–16 sp Android, line height 1.45–1.6.
- Utility/meta: 12–13 px, never below 12 px for essential content.
- Page title: 24–32 px depending on surface; commerce screens avoid oversized display headings.
- Weights: 400 body, 500 labels, 600 actions and section titles, 700 page/product emphasis.
- Prices use tabular numerals when available. Preserve correct Vietnamese diacritics.

## Space, shape, and elevation

- Base spacing unit: 4 px/dp. Preferred steps: 4, 8, 12, 16, 20, 24, 32, 40, 48.
- Radius: 6 controls, 10 compact panels, 14 feature media, full radius only for badges/chips/avatar circles.
- Use dividers and aligned whitespace before boxes. Do not put every group in a card.
- Elevation 1: sticky bars and lifted product actions. Elevation 2: menus and popovers. Elevation 3: dialogs. Ordinary content remains flat.

## Layout and breakpoints

- `sm` 640 px, `md` 768 px, `lg` 1024 px, `xl` 1280 px, `2xl` 1440 px.
- Storefront starts mobile-first and becomes denser on larger screens. Typical desktop content max-width is 1280–1440 px.
- Seller and Admin prioritize desktop workflows but replace dense tables with prioritized rows or detail disclosure below `md`.
- Minimum primary touch target: 44 CSS px on web and 48 dp on Android.
- Verify at 360, 768, and 1280 CSS px plus a representative Android handset.

## Core components

### Navigation

Keep search prominent in the storefront. Account, orders, cart, notifications, and seller actions use labels when space permits. Sticky elements must not hide focused controls or checkout content.

### Product cards

Use a stable media ratio, product name, current price, optional compare price/discount, rating and sold count only when data exists, shop/location context where useful, and a clear unavailable state. Avoid excessive badges and nested cards.

### Forms

Use persistent labels, purpose-specific input modes, inline field errors, a concise error summary for long forms, pending states, and duplicate-submit protection. Required fields cannot rely on placeholder text. Server errors must map back to fields or a visible form message.

### Commerce actions

Price and availability lead. Variant selection must make unavailable combinations explicit. Add-to-cart and buy-now remain distinguishable. Checkout shows authoritative totals returned by the backend and explains stale price, stock, shipping, or voucher changes.

### Tables and dashboards

Keep headers visible, numbers aligned, filters discoverable, and row actions scoped. Destructive/bulk actions require explicit selection and confirmation. Charts display real API data, meaningful units, accessible summaries, empty ranges, and no decorative fake series.

### Feedback

Skeletons mirror final geometry. Empty states explain why and offer a useful next action. Errors keep user-entered data when safe and provide retry. Toasts confirm transient actions but never carry the only explanation of a failure.

## Accessibility

- Use semantic HTML and Compose semantics.
- Keep keyboard order logical and focus visibly styled.
- Associate labels, help, and error text with fields.
- Contain and restore focus for dialogs and drawers.
- Provide meaningful image alternatives; decorative media is ignored by assistive technology.
- Target WCAG AA contrast for essential text and controls.
- Respect reduced motion. Motion clarifies state change and never blocks interaction.

## Voice and content

Use concise, polite Vietnamese. Prefer concrete verbs: `Thêm vào giỏ`, `Đặt hàng`, `Xác nhận đóng gói`. Explain consequences before destructive actions. Never invent urgency, scarcity, ratings, policies, prices, or promotional claims.
