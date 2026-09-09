# Findings and remediation evidence

Date: 2026-09-09. Baseline: `4f5cd0044b6e3872accdd96f5dade7ffd1e476bd`. All findings below are **OPEN**; adding a test or documenting a defect does not remediate it.

Evidence labels: **Reproduced** = exercised locally against real application methods and/or its security chain with mocked persistence/providers; **Source-confirmed** = directly visible in source/configuration; **Risk** = plausible failure requiring PostgreSQL, deployment, or provider testing. Severity is an engineering prioritization, not a calculated CVSS score. Multiple failing probes may represent one finding.

## Critical findings

### SEC-001 — Client can fabricate suc cessful payment

**Critical · Reproduced · Owner: payments/security · Phase 0**

Source: [PaymentController.java](../../src/../src/main/java/com/foalrider/modules/payment/controller/PaymentController.java) `confirmPayment`; [PaymentServiceImpl.java](../../src/../src/main/java/com/foalrider/modules/payment/service/PaymentServiceImpl.java) `confirmPayment`.

`POST /api/v1/payments/confirm/{paymentIntentId}` requires only authentication. The service finds an order by intent ID and unconditionally sets `PAID` and `CONFIRMED`. It does not check ownership, retrieve Stripe state, check amount/currency, or validate the order lifecycle. A customer can obtain their own intent ID from create-intent and confirm without paying. Another customer's order can also be changed if its intent ID is known; guessing the ID is not required for the own-order exploit. Cancelled, refunded, and delivered orders can be reset to confirmed.

Evidence: HTTP probes for unpaid confirmation, other-customer confirmation, and three lifecycle regressions fail. Fix by removing client authority to mutate payment status; validate authoritative provider results against a stored attempt. Acceptance: caller assertions never cause payment/fulfillment, even with a valid customer JWT.

### SEC-002 — Database credential committed to source

**Critical · Source-confirmed; credential validity/exposure untested · Owner: infrastructure/security · Phase 0**

Source: `src/main/resources/application-dev.yml`, datasource block. A concrete remote database URL, username, and literal password are committed. Values are intentionally omitted here. Default configuration activates `dev`, which overrides environment-style defaults from the base file and enables Hibernate schema updates against that remote database.

Assume the committed credential is exposed until the owner confirms revocation. Rotate it, review database access/activity, remove literals from working tree, and coordinate history cleanup after rotation. Ensure local startup uses an explicit isolated database and a nonprivileged application account. No connection to the configured remote database was attempted.

## Identity and API security

### SEC-003 — Unsafe environment and demo-account defaults

**High · Source-confirmed; deployment exposure conditional · Owner: platform · Phase 0–1**

Sources: `application.yml` active-profile/JWT defaults; `application-dev.yml` reusable JWT secret, SQL binding logs, `ddl-auto=update`, disabled Flyway; `ProductDataSeeder.java` dev/test known-password accounts; `db/migration/V4__seed_test_data.sql` demo account inserts in the production migration location.

Production explicitly requires a JWT secret, which is good, but omitting the profile selects unsafe development behavior. V4 contains known-login privileged fixtures and is not isolated by profile. Its current schema failure does not make the seed safe after migration repair. Require explicit environment configuration, remove demo data from the production chain, validate signing keys at startup, and test that production never seeds logins or permits schema auto-update.

### SEC-004 — Existing JWT authenticates a deactivated account

**High · Reproduced · Owner: identity · Phase 0–2**

Sources: `security/jwt/JwtAuthenticationFilter.java` `doFilterInternal`; `CustomUserDetailsService.java` `loadUserById`; `UserServiceImpl.java` `deactivateUser`.

The filter reloads the user but constructs an authenticated token without inspecting `isEnabled()` or account-lock status. DAO login and refresh check status; normal JWT requests do not. The HTTP probe issues a token, disables the account in the repository fixture, and confirms the request still reaches its controller. Apply account-state checks in the filter/shared authentication path and test active-to-disabled transitions immediately.

### SEC-005 — Passwords can be reflected in validation errors and logs

**High · Response reproduced; logging source-confirmed · Owner: API/security · Phase 0**

Source: `shared/exception/GlobalExceptionHandler.java`, both validation handlers. `rejectedValue` copies submitted values into JSON; logging the validation exception message can also include them. A registration request with an invalid short password returns that password in its error body. Redact sensitive fields universally, avoid logging raw binding exceptions, and use safe field/code metadata. Capture logs as well as responses in acceptance tests.

### SEC-006 — JWT claim requirements and session invalidation are incomplete

**High · Issuer/expiry probes reproduced; revocation gaps source-confirmed · Owner: identity · Phase 2**

Sources: `JwtTokenProvider.java` `validateToken`; `AuthServiceImpl.java` `logout`, `logoutAll`, `resetPassword`; `JwtAuthenticationFilter.java`.

Signed tokens with an unrelated issuer or no expiration validate successfully. Signature and normal expiration checks do work. Access tokens carry no session version or revocation reference, so reset/logout-all cannot immediately invalidate already-issued access tokens. Normal password changes do not revoke refresh tokens either (see SEC-007). Define required issuer, subject, expiry, audience where relevant, algorithm/key policy, and revocation semantics. Test key rotation and compromise recovery, not just token issuance.

### SEC-007 — Token lifecycle is not atomic and password changes preserve sessions

**High · Password-change omission reproduced; races are a Risk · Owner: identity · Phase 2**

Sources: `AuthServiceImpl.java` `refreshToken`, `changePassword`, `resetPassword`; `UserServiceImpl.java` `changePassword`; `RefreshTokenRepository.java`; user/token entities lack version/locking fields.

Both password-change services save the new hash without revoking refresh sessions. Refresh consumption reads, checks, then revokes without a lock or conditional consume, allowing concurrent requests to potentially mint multiple descendants. Reset/verification consumption likewise needs concurrency tests. A sequential reset does hash-match a stored token, check expiry, clear it, and revoke refresh sessions; that positive control passes. Introduce atomic consume, refresh families/replay handling, and consistent session invalidation.

### SEC-008 — Role model and privileged assignment lack a consistent boundary

**High · Source-confirmed · Owner: identity/admin · Phase 2**

Sources: `SecurityConfig.java`, `DataInitializer.java`, V1 roles migration, `CustomUserDetails.java`, `UserServiceImpl.java` `adminUpdateUser`, controller `@PreAuthorize` rules.

Filter-level `/admin/**` permits ADMIN/SUPER_ADMIN, but most methods require ADMIN only; no role hierarchy supplies ADMIN to SUPER_ADMIN. STAFF/VENDOR/permission names differ between seed paths. Stored permission arrays are not converted into granted permissions. Any ADMIN able to update a user can assign any existing role ID; no protected-role or last-admin safeguard is present. This is not proof that anonymous users have admin access: method security is enabled and the customer-refund denial test passes. Specify role/permission semantics and enforce protected operations in services with an explicit authorization matrix.

### SEC-009 — Moderation and catalog visibility bypasses

**Medium · Pending review reproduced; catalog paths source-confirmed · Owner: catalog/reviews · Phase 2/5**

Sources: `ReviewServiceImpl.java` `getReview`; `ProductServiceImpl.java` direct ID/slug/SKU methods; `BrandServiceImpl.java` direct lookup/search and `BrandRepository.java`; `CategoryServiceImpl.java` direct lookups; `PricingService.java` `getProductPrice`.

Public review lookup returns pending/rejected content and author identity for a known ID. Public direct catalog/pricing lookups do not consistently enforce active status, although normal product lists filter active products. Share a public-visibility predicate across every route and reserve preview access for owners/moderators/admins. Avoid exposing full author names where a display name is sufficient.

### SEC-010 — Verification bootstrap and password policy are inconsistent

**High · Reproduced and source-confirmed · Owner: identity · Phase 2**

Sources: `AuthServiceImpl.java` `register`; auth/user password DTOs; `SecurityConfig.java` BCrypt encoder. Registration returns active tokens without generating/sending initial verification. Verification can be requested separately, but no purchase verification policy is enforced. Duplicate ChangePasswordRequest classes express different validation and the controller uses the less restrictive user DTO.

The pinned BCrypt implementation accepts different password suffixes after the same first 72 bytes; a local probe reproduces this. Registration allows up to 100 characters, and multibyte input reaches 72 bytes sooner. This matches [Spring's CVE-2025-22228 advisory](https://spring.io/security/cve-2025-22228/). Upgrade and enforce a deliberate byte-length/encoder policy with migration compatibility; do not silently truncate. Send verification on registration and decide which business actions require it.

### SEC-011 — No application abuse controls or trustworthy client-IP boundary

**High · Source-confirmed absence; deployment controls unknown · Owner: platform/security · Phase 2**

Sources: security configuration; auth/controllers; `AuthServiceImpl.java` `getClientIp`; `API_DOCUMENTATION.md` rate-limit section. No implemented rate limiter, login backoff, checkout throttling, upload quota, or shared abuse policy was found. The documentation states limits that source does not enforce. `X-Forwarded-For` is accepted directly for stored client IP and can be spoofed without a trusted proxy policy. Protect business flows with actor/IP/device-aware limits, uniform recovery responses, bounded session counts, and edge controls tested together with app controls.

### SEC-012 — Unbounded or incomplete request validation

**High · DTO probes reproduced; resource exhaustion is a Risk · Owner: API · Phase 2**

Sources: cart DTOs/service quantity arithmetic; `PricingController.java`; review request DTOs; `CreateOrderRequest.java`; explicit `PageRequest.of` and `limit` parameters; `AdminUpdateUserRequest.java`.

`Integer.MAX_VALUE` passes cart quantity validation; additions/totals use unchecked `int` arithmetic. Billing address and review image constraints do not cascade. Pricing requests lack validation for null lists/region, quantities, batches, and amounts. Explicit page/limit routes bypass the unused application max-size setting; Spring's pageable resolver defaults do not cap manually constructed PageRequest values. Several bodies/lists/maps/notes/URLs lack appropriate bounds or allowlists. Add HTTP-level negative and fuzz tests, checked arithmetic, domain caps, safe sorting, numeric precision limits, and constraints on persisted data.

### SEC-013 — Upload validation trusts client MIME and lacks lifecycle controls

**Medium · Source-confirmed; provider behavior untested · Owner: media/platform · Phase 2/5**

Sources: `CloudinaryImageUploadService.java` `validateFile`, `uploadImage`, `uploadImages`; `ImageUploadController.java`. Validation uses declared MIME and size only, then forwards bytes to Cloudinary as an image. Provider image parsing may reject invalid images, so this is not a demonstrated arbitrary-file execution issue. No pixel/dimension/decompression limit, per-user quota, persisted asset owner, orphan cleanup, or transformation-dimension bound is defined. Multi-upload suppresses per-file failures and may report overall success. Define file signatures/decode rules, transformation allowlists, quotas, asset IDs, lifecycle, and explicit partial results. Align servlet multipart limits with the service's stated 10 MB limit.

### SEC-014 — Error handling maps routine client faults to 500

**Medium · Reproduced · Owner: API · Phase 2**

Source: `GlobalExceptionHandler.java` generic fallback. Malformed JSON and missing Stripe-Signature both produce 500 in the real MVC/security slice; missing signature does not process a payment, but the status is wrong. IllegalArgumentException/IllegalStateException used for business errors also fall into the generic handler. Preserve safe messages while explicitly classifying malformed bodies, missing headers, conflicts, constraint violations, unsupported methods/media, and oversized uploads. Do not return raw provider errors to customers.

## Payment, checkout, and money integrity

### PAY-001 — Webhooks do not bind complete payment identity or enforce event order

**High · Reproduced · Owner: payments · Phase 4**

Source: `PaymentServiceImpl.java` success/failure handlers. Signed events identify orders from metadata; handlers do not compare the event intent with the order's stored intent, expected amount/currency, or merchant/mode. Failure events can overwrite PAID; success can reopen cancelled/refunded orders. Probes reproduce old-intent acceptance, insufficient amount, wrong currency, failure-after-success, and cancelled-order reopening. These tests use locally signed synthetic events and establish handler behavior, not an ability to forge real Stripe signatures. Verify the stored attempt and enforce a transition policy with reconciliation for late success. [Stripe documents unordered and duplicate delivery](https://docs.stripe.com/webhooks).

### PAY-002 — Event processing can silently lose a successful payment update

**High · Reproduced for service storage exception; transaction-dependent details remain · Owner: payments/platform · Phase 4**

Sources: `PaymentServiceImpl.java` catch-all blocks/deserializer early return; `WebhookController.java`. A synchronous repository save exception is logged and swallowed; the controller may acknowledge success despite lost processing. Real JPA rollback-only/commit-time exceptions can instead escape and trigger the controller's blanket 400; PostgreSQL tests must cover both. API-version/deserialization failures return normally. Introduce durable inbox acceptance, error classification, replay, and alerting; acknowledge only persisted work.

### PAY-003 — Payment intent creation has no durable idempotency or attempt history

**High · Source-confirmed; duplicate charging/failure races are a Risk · Owner: payments · Phase 4**

Source: `PaymentServiceImpl.java` `createPaymentIntent`. Repeated requests while order remains PENDING can create a new provider intent and overwrite the stored ID, including when payment status is PROCESSING. No Stripe RequestOptions idempotency key is supplied. Network calls occur inside a DB transaction; remote success followed by local failure is not reconciled. Store attempts/operation keys, reuse eligible intents, and reconcile ambiguous outcomes. [Stripe's idempotency API](https://docs.stripe.com/api/idempotent_requests) supplies a provider mechanism; application uniqueness is still required.

### PAY-004 — Cancellation/refund paths disagree about money and stock

**High · Status-only refund reproduced; other paths source-confirmed · Owner: orders/payments · Phase 3–4**

Sources: `OrderServiceImpl.java` cancellation/admin status updates; `PaymentServiceImpl.java` `processRefund`, `charge.refunded` handling. Customer cancellation of a paid confirmed order releases stock without initiating/referring to a refund. An admin status transition marks REFUNDED without contacting Stripe. Provider refund immediately marks order REFUNDED without checking pending/failed outcome, does not restore stock, and dashboard refund events are merely logged. Define refund/return states and money reconciliation; do not use order status as proof of a refund or physical return.

### ORD-001 — Missing variant bypasses inventory consumption

**High · Reproduced · Owner: inventory/checkout · Phase 3**

Sources: `CartServiceImpl.java` `getAvailableStock`; `OrderServiceImpl.java` stock helpers. Cart accepts missing variant for products with variants, using summed stock; checkout validates and decrements only items with a selected variant. Products without variants default to effectively unlimited stock. The checkout probe with a variant-managed product and null selected variant succeeds even when variant stock is zero. Reject incomplete variant selections and explicitly model inventory policy.

### ORD-002 — Overselling, duplicate checkout, and repeat stock release are possible under concurrency

**High · Source-confirmed unsafe pattern; concurrency impact requires PostgreSQL proof · Owner: inventory/checkout · Phase 3**

Sources: `OrderServiceImpl.java` `createOrder`, `updateStock`, `restoreStock`; cart/order/variant entities. Reads followed by entity saves have no lock/version and no atomic reservation. `Math.max(0, newStock)` hides negative outcomes; it does not prevent overselling. The repository already has conditional `decrementStock`, but checkout never calls it. Cart checkout and cancellation have no idempotency key or atomic claim. Implement atomic reservation, cart claim, unique release operations, and tests synchronized against the last available SKU. Mock tests are not claimed as concurrency proof.

### ORD-003 — Checkout does not revalidate saleability or expire reservations

**High · Inactive product reproduced; expiry omission source-confirmed · Owner: checkout · Phase 3**

Source: `OrderServiceImpl.java`. Items added while active can be checked out after deactivation; stock is deducted before payment with no reservation expiry/cleanup worker. Unpaid abandoned orders can hold inventory indefinitely. Recheck product/variant policy, create bounded reservations, and release/consume each exactly once. Coordinate expiry with payment attempts and late webhook handling.

### ORD-004 — Order numbers depend on process-local counter

**Medium · Source-confirmed · Owner: orders · Phase 3**

Source: `OrderServiceImpl.java` `generateOrderNumber`. Counter initializes from current time modulo 100000 and wraps modulo 100000 per date. Multiple instances/restarts or high daily order volume can collide. Database uniqueness prevents duplicate rows but turns collisions into failed checkout. Replace with durable database allocation and test restarts/multiple instances.

### PRICE-001 — Cart, order, payment, and invoice have different pricing authorities

**High · Stale-price checkout reproduced; other differences source-confirmed · Owner: pricing · Phase 3**

Sources: `CartServiceImpl.java` mapping; `CartItem.java`; `OrderServiceImpl.java`; `PricingService.java`; `PaymentServiceImpl.java`; `InvoiceServiceImpl.java`.

Cart totals use regional product pricing without variant adjustments, while line DTOs retain stored prices. Orders copy stored cart prices, add hardcoded tax/shipping, and store no currency. Payments use global Stripe currency; invoices print dollars. A cart line stored at 1.00 checks out at 1.00 after the product price is 100.00. Define whether quotes lock prices or require re-acceptance, then persist the complete quote snapshot and use it consistently across all systems.

### PRICE-002 — Inclusive tax is added again; converted totals mix units

**High · Inclusive tax reproduced; currency paths source-confirmed · Owner: pricing · Phase 3**

Source: `PricingService.java` `calculateFullPricing`, `getProductPrice`, `calculateShipping`; `TaxRate.java`. TaxRate correctly extracts the inclusive portion, but full pricing adds all tax to subtotal regardless of inclusive flag. A synthetic 120.00 tax-inclusive line with 20% included tax and free shipping becomes 140.00. Fallback regional pricing labels base values with a local currency without converting them. Currency overrides convert items but not fixed shipping/threshold units consistently; totalWeight is always zero. Unify unit-aware calculations and property tests; determine real tax rules with the business before enabling destinations.

### PRICE-003 — Stripe amount conversion assumes two decimals and truncates

**High · Source-confirmed · Owner: payments/pricing · Phase 3–4**

Source: `PaymentServiceImpl.java` amount `multiply(100).longValue()`; `Currency.java` `toStripeAmount`. The existing currency-specific helper is unused by payments and also silently truncates. Zero-decimal currencies and excess precision/overflow are not safely handled. Store currency with each order, apply supported currency rules, validate limits, and use exact conversion after deliberate rounding. [Stripe currency documentation](https://docs.stripe.com/currencies) describes minor-unit exceptions.

## Data and runtime reliability

### DATA-001 — Migration chain does not build the application schema

**High / release blocker · Source-confirmed; clean PostgreSQL execution pending · Owner: data/platform · Phase 1**

Sources: all five migrations and all 24 `@Table` entities. Only roles, users, refresh_tokens, and addresses have CREATE TABLE migrations. V4 inserts user `region_code`/`locale` columns absent in V2 and inserts catalog tables never created by preceding migrations. User preferred currency and inherited refresh token `updated_at` also lack corresponding migration coverage. `prod` runs Flyway with Hibernate validation; `dev` hides drift with schema update and Flyway off. Repair both fresh and existing database paths without casually modifying applied checksums.

### DATA-002 — Database constraints and reference lifecycle are incomplete

**High · Source-confirmed mapping gaps; production schema unknown · Owner: data/domain · Phase 1/3/5**

Sources: entity constraints, V1–V5, `UserServiceImpl.java`/`ProductServiceImpl.java` hard deletion. No version fields or comprehensive CHECK constraints protect money/quantities. A nullable variant in a composite unique cart key requires deliberate PostgreSQL null handling. Default-address index is not unique; count/check/reset races may exceed limits or create multiple defaults. Hard product/user deletes can conflict with order references; order responses still dereference live products despite storing snapshots. Add constraints, safe archival/retention, immutable order snapshots, and delete/concurrency integration tests.

### REL-001 — Read-only reads create persistent state

**High · Source-confirmed; PostgreSQL behavior pending · Owner: cart/notifications · Phase 5**

Sources: `CartServiceImpl.java` `getCart` -> `getOrCreateCart`; `NotificationServiceImpl.java` `getPreferences` -> `createDefaultPreferences`. Both read-only transactional methods may save new rows. PostgreSQL read-only transactions or Hibernate flush behavior can cause failures/nonpersistence, hidden by mocked unit tests. Make GET side-effect-free or create state in explicit write transactions, with concurrency-safe uniqueness.

### REL-002 — Invoice rendering lacks a transaction/fetch boundary

**High · Source-confirmed; persistence reproduction pending · Owner: orders · Phase 5**

Sources: `InvoiceServiceImpl.java` `generateInvoiceById`/`generateInvoiceByOrderNumber`; order lazy items; `open-in-view: false`. The service loads an order through plain repository lookup, then renders lazy items outside a service transaction. The controller's earlier ownership lookup is a separate operation. This is a likely LazyInitializationException on persisted orders; invoice ownership itself is checked by customer controllers and is not reported as an HTTP IDOR. Fetch an authorized invoice DTO transactionally and render from detached values. Honor billing details, currency, and immutable invoice identity.

### REL-003 — Notifications and email are not a reliable delivery pipeline

**High · Source-confirmed · Owner: notifications/platform · Phase 5**

Sources: `EmailServiceImpl.java`, `NotificationServiceImpl.java`, application/config annotations, caller search. There is no `@EnableAsync`; methods annotated async therefore do not establish background execution. Order/payment/review notification helper methods are not invoked by those flows. No outbox, durable retry/dead letter, provider status, or configured bounded SMTP timeouts exists. Preferences are stored but not used to decide delivery. HTML templates interpolate user names without escaping. Use transactionally recorded events, bounded workers, safe templates, retries and truthful delivery status.

### REL-004 — Query fan-out and unbounded analytics do not scale

**Medium · Source-confirmed; load impact unmeasured · Owner: data/performance · Phase 5–6**

Sources: `ProductServiceImpl.java` `mapToResponse` runs three additional repository queries per product; category/brand product counts load collections; review maps query votes per row; `AdminDashboardServiceImpl.java` uses findAll/unpaged queries; review purchase checks load order histories. Public product ID GET also increments a DB counter. Use aggregate projections, batch fetches, bounded report ranges, query budgets, and controlled analytics writes. Measure query counts and plans against representative data.

### REL-005 — Redis configuration does not match advertised behavior

**Medium · Source-confirmed · Owner: platform · Phase 1/6**

Source: `RedisConfig.java` custom factory and `application-dev.yml` auto-configuration exclusion. Base configuration sets redis.host, satisfying the custom bean condition even when dev excludes Redis auto-configuration. Custom construction does not apply Boot's declared timeout/TLS settings. No cache consumers or cache annotations were found. Choose whether Redis is required, optional, or removed; configure authenticated encrypted connections where deployed, short timeouts, explicit readiness and fallback behavior, then test failures.

## Feature completeness and operational controls

### CAT-001 — Catalog update and category integrity gaps

**Medium · Source-confirmed · Owner: catalog · Phase 5**

Sources: `ProductServiceImpl.java` update/create helpers; `CategoryServiceImpl.java` update. Product update accepts images/variants but does not apply them; create saves children without synchronizing parent collections. Slug collision handling is inconsistent; nullable fields cannot reliably be cleared. Only self-parenting is rejected, so ancestor cycles are possible. Provide deliberate create/update DTOs, child reconciliation, version conflicts, ancestry checks, and clear/remove semantics.

### REV-001 — Rating and vote aggregates can drift

**Medium · Source-confirmed; concurrency proof pending · Owner: reviews · Phase 5**

Source: `ReviewServiceImpl.java` update/vote/moderation. Editing an approved review resets it to pending without recalculating product rating. Vote counters are read-modify-write without concurrency protection. Recompute on all visibility transitions and derive/atomically maintain aggregates; verify concurrent votes and moderation.

### BIZ-001 — Coupon and reporting fields imply unavailable behavior

**Medium · Source-confirmed · Owner: product/reporting · Phase 3/5**

Sources: `OrderServiceImpl.java` coupon storage; `AdminDashboardServiceImpl.java`; unused sold-count update repository method. Coupons are accepted/stored without validation or discount calculation. Revenue includes unpaid/failed orders unless their order status is cancelled/refunded; several metrics are literal zero/empty placeholders. Product sales counters are not updated by completed sales. Reject unsupported features or mark them unavailable, and base reports on settled financial facts.

### OPS-001 — Release, monitoring, recovery, and security audit controls are missing from repository

**High · Repository gap; deployed controls unknown · Owner: platform · Phase 1/6**

No committed CI pipeline, container/deployment definition, Maven wrapper, SBOM gate, enabled integration test suite, JaCoCo configuration, restore drill, incident runbook, or durable admin/security audit log was found. Timestamp auditing alone is not an action audit trail. Actuator exists, but no Prometheus registry dependency or custom business metrics is configured. Define SLOs, readiness/liveness, graceful drain, dependency budgets, alert routing, backups/PITR, restore rehearsal, and release evidence. Do not infer a deployed WAF, database RLS, or TLS policy from this source tree.

### TEST-001 — Existing tests and documentation overstate assurance

**High · Reproduced/source-confirmed · Owner: QA/platform · Phase 1/6**

Baseline: 137 enabled unit tests pass using a startup-agent workaround; one application test is disabled. That test's profile file also contains invalid `spring.profiles.active` placement and disables Flyway. Existing tests mock repositories/static security helpers; no order-service, pricing, invoice, upload, security-chain, or PostgreSQL concurrency suite existed. Historical documents alternately claim 45.5%, 96.3%, and production readiness; these are not current coverage measurements. README references a nonexistent `./mvnw`. Use the new evidence and mandatory security assertions rather than inherited claims.

### DEP-001 — Resolved dependency baseline has substantial advisory matches

**High · Automated version matches; application reachability requires triage · Owner: platform/security · Phase 1**

`mvn dependency:tree` resolves Boot 3.2.1, Framework 6.1.2, Security 6.2.1, Tomcat 10.1.17, PostgreSQL JDBC 42.6.0, and other old dependency versions. After explicit user approval, OSV was queried for 164 public Maven coordinates: 28 packages matched 108 distinct advisory IDs; 24 matched packages are compile/runtime and four are test scope. These are **not 108 proven exploitable vulnerabilities** and IDs can overlap underlying CVEs. See [dependency evidence](evidence/dependency-osv.json) and [dependency triage](DEPENDENCIES.md).

One applicable BCrypt issue is locally reproduced (SEC-010). Avoid blanket claims of RCE/SSRF: [Spring's URL parsing issue](https://spring.io/security/cve-2024-22259/) requires external URL parsing plus host validation, and no such path was found; [the WebFlux resource bypass](https://spring.io/security/cve-2024-38821/) requires WebFlux, whereas this app uses MVC. [Tomcat advisories](https://tomcat.apache.org/security-10.html) and [pgJDBC advisories](https://jdbc.postgresql.org/security/) have their own configuration conditions. Upgrade compatible dependency families, triage runtime reachability, and rescan the built artifact.

## Controls that are already useful

- BCrypt password hashing and SHA-256 storage of random refresh/reset/verification tokens; reset expiry and sequential refresh revocation checks.
- Signed JWT validation rejects wrong signatures, expired tokens, malformed strings, and unsigned JWTs in the probes.
- Method security is enabled; customer refund access and anonymous payment confirmation are denied in HTTP tests.
- Ownership predicates/checks exist for cart items, addresses, customer orders/invoices, notification changes, and review edits.
- Stripe raw-payload signature verification rejects invalid signatures; duplicate success is guarded when the order is already PAID.
- Parameterized Spring Data queries, DTO response mapping, BigDecimal use, open-in-view disabled, production schema validation and disabled production Swagger are good foundations.

These strengths narrow particular attack paths. They do not compensate for the critical payment mutation or missing commerce invariants.
