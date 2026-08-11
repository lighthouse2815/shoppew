---
name: shoppew-ui
description: Design, implement, review, or polish shoppew storefront, Seller Center, Admin, and Android commerce interfaces using the repository's original design system. Use for shoppew routes, React components, CSS, design tokens, Jetpack Compose screens, responsive behavior, accessibility, forms, tables, product cards, checkout, dashboards, and loading, empty, error, disabled, or success states.
---

# Shoppew UI Workflow

Create an original, compact, trustworthy Vietnamese marketplace experience. Never invoke or imitate a global UI/interface-design skill for this repository.

## Start from repository truth

1. Read `AGENTS.md` and `docs/DESIGN_SYSTEM.md`.
2. Inspect the existing route, component, API call, data contract, and neighboring screens before changing UI.
3. Identify the user's job, primary action, irreversible actions, and all data states.
4. Reuse repository tokens and primitives. Extend tokens centrally only when an existing semantic token cannot express the requirement.

## Preserve the product language

- Use the lowercase `shoppew` wordmark and the documented teal, chartreuse, violet, coral, and neutral palette.
- Use Vietnamese as the demonstrated locale, VND for money, and Asia/Ho_Chi_Minh for displayed business time.
- Keep storefront layouts commerce-dense without clutter. Prioritize product comparison, price, stock, shipping, vouchers, and trust signals.
- Keep Seller Center and Admin task-dense. Prefer clear filters, bulk-safe actions, tables on wide screens, and scan-friendly stacked rows on narrow screens.
- Keep Android screens app-native. Respect system bars, back behavior, keyboard insets, touch targets, and state restoration.
- Do not invent prices, sales counts, ratings, testimonials, contact details, guarantees, or campaign claims. Render real API or seeded data.
- Do not copy Shopee or another marketplace's logo, header, orange palette, layout, assets, copy, or trade dress.

## Build every state

For each data-driven surface, implement as applicable:

- initial loading and stable skeletons;
- empty state with a useful next action;
- recoverable error state with retry;
- field-level and summary validation;
- pending and duplicate-submit protection;
- disabled state with a discoverable reason;
- success feedback and resulting navigation/state update;
- unavailable, stale-price, out-of-stock, or permission-denied state.

Never leave a clickable control without a real behavior. Backend authorization and validation remain authoritative.

## Compose the interface

- Use semantic hierarchy before decoration. A page needs one clear primary action, not a field of equal buttons.
- Use flat sections, dividers, and alignment for density. Do not wrap every group in a rounded card.
- Reserve elevation for overlays, menus, sticky commerce actions, and meaningful layer changes.
- Use one icon family per client and include text labels when an icon's meaning is not universal.
- Keep headings compact; product/catalog pages are not marketing landing pages.
- Keep focus visible, labels explicit, errors associated with fields, dialogs focus-contained, and keyboard order logical.
- Target at least 44 CSS px touch targets on web and 48 dp on Android for primary controls.
- Avoid layout shifts: reserve media dimensions and keep skeleton geometry close to loaded content.

## Responsive behavior

- Design mobile storefront behavior first, then add tablet and desktop density.
- Convert wide tables to prioritized rows, disclosure panels, or horizontal regions with clear affordances; never merely shrink text.
- Keep cart and checkout totals visible without covering form content.
- Test at 360 px, 768 px, 1280 px, and a representative Android phone viewport.
- Treat hover as enhancement; all actions must work with touch and keyboard.

## Verify before reporting

1. Run the affected client's lint, typecheck, and tests.
2. Exercise the real route against the backend or an explicit local provider.
3. Check loading, success, empty, validation, and error behavior.
4. Inspect narrow and wide layouts, keyboard navigation, focus, and console/network failures.
5. Report only checks actually run. Record verified milestones in `docs/IMPLEMENTATION_STATUS.md`.
