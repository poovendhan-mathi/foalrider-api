# Implementation plan

Date: 2026-09-09. Status: proposed work, not implemented. Use finding IDs in [FINDINGS.md](FINDINGS.md) and update [PROGRESS_TRACKER.md](PROGRESS_TRACKER.md) with evidence when each gate passes.

## Delivery sequence

Keep the modular monolith. Make security, money, inventory, and database behavior correct before adding growth features or splitting services. Each work package should be a focused PR with tests, migration impact, operational instructions, and a rollback or forward-repair plan.

| Phase | Deliverable | Depends on | Rough engineering effort* |
| --- | --- | --- | --- |
| 0 | Contain exposed credentials and unsafe payment confirmation | None | 1–2 days plus credential-owner actions |
| 1 | Reproducible database/build and dependency baseline | 0 | 3–5 days |
| 2 | Authentication, authorization, validation, and abuse controls | 1 | 4–7 days |
| 3 | Authoritative pricing, checkout, and inventory reservations | 1–2 | 6–10 days |
| 4 | Payment attempts, webhook inbox, refunds, reconciliation | 3 | 6–10 days |
| 5 | Fulfillment, notifications, catalog, reporting, audit records | 3–4 | 5–8 days |
| 6 | Failure, load, recovery, and release qualification | All launch-critical work | 4–7 days |

*Planning estimates for an engineer familiar with Spring/PostgreSQL, not a commitment. Allow additional time for frontend contracts, existing data repair, provider configuration, and independent security review. Run targeted testing in every phase; phase 6 is not the first security test.

## Phase 0 — containment

1. SEC-002/003: inventory all committed credential locations without copying values into tickets. Rotate the exposed database credential at its owner, investigate its use, replace local/deployed secrets, revoke affected JWT signing material and seeded sessions where applicable. Deleting text is not rotation. Coordinate repository-history cleanup after revocation; do not rewrite shared history blindly.
2. SEC-001: remove the mutation from `/payments/confirm/{paymentIntentId}`. A frontend completion callback may only read an ownership-scoped status. Temporary server verification must retrieve the stored intent and verify success, amount, currency, merchant/mode, and allowable lifecycle before changing state. Treat webhook confirmation as the long-term authority.
3. Block fulfillment of orders whose paid status cannot be reconciled with Stripe. Identify affected orders in an authorized staging/production reconciliation exercise; do not automatically cancel or refund them.
4. SEC-004/005: reject inactive/locked principals in the JWT filter; remove rejected secrets from validation responses and logs. Add negative endpoint tests before rollout.

Acceptance: no client-only payment mutation; unauthorized callers cannot affect an order; inactive JWT rejected; secrets rotated with owner evidence; password values absent from response and captured logs. Preserve audit records of investigation and decisions.

## Phase 1 — build and database foundation

1. Add a pinned Maven wrapper, Java 21 toolchain/enforcer configuration, and CI with explicit profiles. Align Mockito/Byte Buddy versions and supply the agent at JVM startup where required. Add JaCoCo with compatible pinned versions; measure coverage before setting a numerical ratchet.
2. DATA-001: compare a sanitized deployed schema with all entity mappings. Build a clean install path and an upgrade path separately. Include all commerce/pricing tables, user locale columns, and inherited `refresh_tokens.updated_at`.
3. Decide how to handle the already-broken V4 migration based on whether it has ever been applied. Preserve applied checksums. Use a reviewed baseline for existing databases and a correct migration lineage for fresh installs; simply appending V6 does not repair a fresh install that fails at V4.
4. Move demo data into explicit local fixtures. Production migrations must not create known-login customers or admins. Provision the first privileged account through a one-use operational process.
5. Add database constraints for positive line quantities, nonnegative monetary/stock values, unique payment/event identifiers, one active reservation operation, and deterministic uniqueness of nullable-variant cart lines. Add indexes based on actual query plans.
6. DEP-001: upgrade the Boot-managed dependency family to a maintained release selected from current support/advisory evidence. Verify Java, Springdoc, Flyway/PostgreSQL, Hibernate, JWT, Stripe API version, Cloudinary, and PDF compatibility. Generate an SBOM and triage every scanner match by runtime scope and reachability.
7. Add PostgreSQL integration tests using disposable databases, with Flyway enabled and `ddl-auto=validate`. Fix the invalid test profile setting. Mock outbound Stripe, SMTP, and Cloudinary at the adapter boundary.

Acceptance: fresh install and sanitized upgrade both pass; no demo accounts; application startup test enabled; baseline tests pass on a clean machine; dependency inventory and triage attached. Take and restore a backup before any real migration.

## Phase 2 — identity and API boundaries

1. SEC-004/006/007: centralize account-status enforcement and a session/security version or invalid-before timestamp. Reject invalid issuer, missing expiry, malformed subject, wrong key/algorithm, future tokens, and invalid account state. Use a documented key-rotation mechanism.
2. Consume refresh/reset tokens atomically. Introduce refresh token families, replay detection, and explicit replay response. Invalidate affected families on password reset/change, logout-all, account disable, and privilege changes according to policy. Add concurrent consumption tests against PostgreSQL.
3. SEC-008: define permissions once, reconcile role seed names, and enforce privileged operations in service boundaries as well as controllers. Prevent ordinary admins from assigning protected roles or removing the last privileged account. Test every route as anonymous, customer A, customer B, staff, admin, and super-admin.
4. SEC-010: send verification on registration; define which actions require a verified email. Consolidate duplicate password DTOs/flows. Address BCrypt's 72-byte limit explicitly or migrate through a versioned password encoder with safe rehash-on-login.
5. SEC-011/012: add per-account and per-trusted-client abuse limits for login, registration, reset, resend, refresh, checkout, uploads, pricing batches, and reviews. Return 429 and Retry-After; set a deliberate behavior when the shared limiter is unavailable. Trust forwarded IP only from known proxies.
6. Centralize bounded pagination and sort allowlists; add quantity, total cart size, list/map size, money precision, URL scheme/host, and nested DTO validation. Use checked arithmetic and database constraints. Return 400/401/403/404/409/413/429 consistently, with safe error codes and trace IDs.
7. Test CORS through the real servlet/filter deployment, including preflight. Keep bearer-token CSRF assumptions explicit; re-evaluate CSRF if cookie authentication is introduced.

Acceptance: authorization matrix green; both password-change endpoints enforce identical policy; session compromise recovery tested; unknown/invalid input returns controlled 4xx; no secrets in responses/logs; sustained abuse is bounded.

## Phase 3 — pricing, checkout, and inventory

1. Introduce a money value type carrying amount and ISO currency, plus one quote service. Resolve prices by product **and variant**, region, destination, currency, discount policy, shipping method, weight, and effective date. Use immutable quote snapshots with version/expiry.
2. PRICE-001/002/003: reprice at checkout or reject an expired quote for explicit customer acceptance. Extract inclusive tax without adding it again; add only exclusive taxes. Convert shipping/thresholds and line values consistently. Store currency, tax lines, rounding decisions, address/billing snapshots, and quote version on the order.
3. ORD-001: require a selected active variant for variant-managed products. Explicitly model stockless/digital items rather than assuming unlimited stock. Recheck product/variant status and relationship when placing the order.
4. ORD-002: lock the cart or use a versioned checkout claim. Atomically reserve stock with conditional updates and check affected-row counts; use deterministic SKU lock order. Add reservation rows with expiry and unique release/consume operations. Do not clamp stock to zero to hide conflicts.
5. Add a client idempotency key scoped to actor + operation and a request hash. Same key/same payload returns the original order; same key/different payload returns 409. Make duplicate and concurrent checkout tests prove one order/reservation outcome.
6. Replace in-memory order numbering with a database sequence/unique allocation. Rework cancellation/expiry so stock releases exactly once, and late payment success enters reconciliation instead of silently reopening a cancelled order.
7. Reject unsupported coupon codes until a real eligibility/redemption engine exists. Include atomic redemption reservations when implemented.

Acceptance: cart accepted quote = order = Stripe amount/currency = invoice; concurrent attempts for the last unit produce one reservation; retries do not create extra orders; abandoned orders release stock; payment/cancellation races preserve money and stock invariants.

## Phase 4 — payment processing

1. Introduce a `PaymentGateway` adapter and `payment_attempts` table with expected amount/currency, provider ID, order version, status, and idempotency key. Reuse the current eligible intent; do not overwrite history. Keep network calls outside long database transactions, using durable operation records and reconciliation for ambiguous results.
2. Add a webhook inbox with unique provider + event ID, payload digest, provider creation time, receipt time, processing state, retry count, and last safe error. Verify raw-body signatures before storing. Acknowledge only after durable acceptance; asynchronous processing is safe only after that commit.
3. Verify event mode/account, stored attempt identity, amount received, currency, and order state. Deduplicate event IDs and enforce idempotent business operations even for distinct events about the same payment. Handle out-of-order success/failure and old attempts explicitly.
4. Distinguish invalid signatures (4xx), temporarily unavailable storage (retryable failure before durable acceptance), unsupported events (recorded ignore), and deserialization/API-version problems (quarantine + alert, never silent loss).
5. Model refund requests and outcomes separately: pending, succeeded, failed, partial. Use provider idempotency keys. Do not label a refund complete when merely requested. Handle provider-dashboard refunds and disputes. Stock is returned according to physical return disposition, not automatically for every refund.
6. Add a scheduled reconciliation worker for pending/ambiguous payments and refunds. Alert on aged events, amount mismatches, multiple successful attempts for one order, paid-but-unfulfillable orders, and ledger disagreement. Provide an audited operator replay procedure.

Acceptance: signed webhook fixtures, duplicate/out-of-order events, provider timeouts, lost responses, DB failure after provider success, refunds, and cancellation races all converge to a correct durable result. Run Stripe test-mode contract tests only against an isolated authorized account.

## Phase 5 — operations and product completeness

1. REL-001/002: replace read-only methods that create carts/preferences with explicit write operations or side-effect-free empty responses. Fetch complete invoice data within a transaction and render from immutable DTOs outside it.
2. Add transactional outbox records for order/payment/auth/review notifications. Deliver with bounded workers, retries, dead letters, preference enforcement, HTML escaping, and accurate delivery status. Enable/configure async execution only if used; annotations alone do not enable it.
3. Complete variant/image update operations and safe archival of products/users. Preserve order/invoice snapshots and references. Reject category ancestry cycles. Add stock adjustment history, actor/reason, and authorization.
4. Hide non-public catalog/review data from all public read paths. Recalculate review aggregates when an approved review returns to pending. Make rating/vote aggregate updates concurrency-safe.
5. Move dashboard counts/sums into bounded database queries. Count settled revenue using payment/refund facts and currency buckets. Remove fake zero-filled metrics or explicitly label unavailable fields. Use query budgets to eliminate per-product extra queries.
6. Add durable security/admin audit events with actor, action, target, safe before/after fields, request ID, timestamp, and outcome. Restrict access, define retention, and avoid credentials/payment secrets.

Acceptance: transactional notifications recover after outages; invoice works with open-in-view disabled; catalog edits preserve history; reports reconcile to payment records; privileged changes are attributable.

## Phase 6 — release qualification

Execute the complete [test matrix](TESTING.md), including PostgreSQL concurrency tests, authenticated API scanning, provider fault injection, load/soak tests, migration rehearsal, backup restoration, and rollback/forward-repair drills. Establish dashboards and alerts before launch. Obtain business sign-off for supported currencies, shipping destinations, taxes, cancellation/return policy, staff permissions, and data-retention policy.

Release only when critical/high findings are fixed or a narrowly scoped exception has an owner, rationale, compensating controls, expiry, and verification evidence. Payment fabrication, unresolved credential exposure, and overselling are not acceptable launch exceptions.
