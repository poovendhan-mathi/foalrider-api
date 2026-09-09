# Architecture and assessment coverage

## What is implemented

The project is a Java 21 / Spring Boot 3.2.1 MVC modular monolith. Its `/api/v1` servlet context exposes 142 controller mappings across 15 controllers. There are 196 production Java files (~16,145 lines), 24 mapped entity tables, and five SQL migrations. The checked-in migration chain creates only four of those tables. See [source inventory](evidence/source-inventory.json), [route inventory](evidence/routes.json), and [schema coverage](evidence/schema-coverage.json).

PostgreSQL/Hibernate owns users, catalog, carts, orders, review data, preferences, and regional pricing. Stripe is called through static SDK methods. Cloudinary handles media upload/transformation. SMTP handles email. Redis has configuration but no implemented cache consumers. DTOs isolate HTTP responses from entities. Most services wrap persistence operations in transactions, but external side effects and invoice rendering do not have suitable durable boundaries.

## Trust boundaries and invariants

| Boundary | Untrusted input | Required invariant | Current gap |
| --- | --- | --- | --- |
| Browser/mobile → API | JWT, object IDs, quantities, addresses, URLs, request bodies | Authenticated identity and permissions govern every private object/action | Disabled-token checks; payment ownership/status; validation and rate limits |
| API → PostgreSQL | Domain changes and concurrent requests | Stock never oversells; sessions consume once; durable state is internally consistent | Migrations, locks/constraints, reservation/idempotency model |
| Browser → Stripe → webhook | Client completion hints and unordered signed events | Only matched authoritative provider success settles expected money | Client confirmation bypass; incomplete identity/amount binding and ordering |
| API → Stripe | Payment/refund operations with timeout ambiguity | Retry creates at most one intended financial operation | No operation keys, attempt ledger, or reconciliation |
| API → Cloudinary | User file bytes and transformation requests | Bounded validated assets with ownership and lifecycle | MIME-only app checks, quotas, asset registry and cleanup |
| API → SMTP | Names/content and delivery tasks | Escaped content and durable recoverable delivery | Async not enabled; no outbox/retries; unwired domain notifications |
| Administrator → API | Catalog, role, stock, order/refund changes | Least privilege, attributable actions, valid state transitions | Inconsistent roles, unrestricted role assignment, missing action audit |
| Deployment → runtime | Secrets, profiles, proxies, database/Redis endpoints | Explicit safe configuration and repeatable startup/recovery | Dev defaults, committed credential, missing operational evidence |

## Module coverage and practical enhancements

| Area | Java files | Assessment | Main implementation work |
| --- | ---: | --- | --- |
| Auth | 11 | Service/controller/DTO flows traced; reset/password/JWT negative probes added | Atomic token lifecycle, initial verification, consistent password policy, abuse limits |
| User/address | 21 | Role/profile/deletion paths and ownership predicates reviewed | Privilege boundaries, archival/retention, address uniqueness under concurrency, locale updates |
| Product/category/brand | 29 | CRUD, all read paths, child mapping and repository queries reviewed | Public visibility, variant/image mutation, ancestry checks, query efficiency |
| Cart | 11 | Price/stock derivation, DTOs, ownership and creation paths reviewed | Variant selection, overflow bounds, write/read separation, versioned checkout |
| Order/invoice | 20 | Checkout/state/stock/snapshot/invoice paths traced; checkout probes added | Quote snapshots, reservation lifecycle, state machine, invoice fetch boundary |
| Payment | 7 | Controllers, provider calls, raw webhook verification and handlers traced; signed fixtures tested | Remove client authority, attempt/inbox/refund ledger, reconciliation |
| Pricing | 19 | Regional fallback, FX, tax, shipping, currency helpers, seed values reviewed | One pricing engine, inclusive tax, coherent FX/minor units, destination/weight policy |
| Review | 19 | Moderation, ownership, public retrieval, aggregates and votes reviewed | Visibility, cascade validation, aggregate consistency and bounded queries |
| Notification | 17 | Delivery/helper callers, preferences, bulk processing and email templates reviewed | Outbox, bounded delivery, preferences, safe HTML, expiry filtering |
| Upload | 4 | Endpoint guards, validation, SDK options and failures reviewed | Quotas, signature/decode checks, asset ownership and partial-result contract |
| Admin | 9 | Reporting calculations and data access reviewed | Settled revenue, truthful metrics, efficient aggregate queries |
| Platform/shared | 29 | Security, config, DTO/error handlers, utilities, base entity and seeders reviewed | Safe profiles, error redaction, dependency updates, CI/observability |

The review combines manual tracing of controller/service/security/configuration behavior with repository-wide searches of repositories, DTO constraints, entity mappings, side-effect callers, and operational artifacts. File inventory is not a claim of runtime path coverage. Boilerplate getters/builders are generated by Lombok; generated target output is not treated as a separate source of truth. The existing documentation was checked for contracts, design intent, contradictory test claims, and missing implementations.

## Recommended design direction

Retain the modular monolith and its transactional database. Add explicit domain services for quotes, inventory reservations, payment attempts, refund requests, and session management. Put Stripe/Cloudinary/SMTP behind injectable adapters so failure testing does not need static mocks or real accounts. Use a database inbox/outbox to bridge transactions and external side effects. Enforce module APIs and ownership predicates; introduce microservices only if actual scale/team boundaries justify them.

Keep PostgreSQL as the authoritative source for money, stock, and operational state. Redis can support shared rate limits and performance after an explicit failure policy is defined. Do not use cache availability as a substitute for durable payment or inventory guarantees.

## Assumptions that need business decisions

- **One retailer or many isolated businesses?** Current data has no business/tenant ownership key. A VENDOR role does not establish vendor isolation.
- **Consumer retail or B2B purchasing?** Company memberships, buyer approval workflows, negotiated prices, purchase orders, and credit terms are absent.
- **Supported markets and currencies?** Regional seed data is demonstration configuration, not validated tax/shipping policy.
- **Inventory policy?** Backorders, reservations, multiwarehouse stock, returns-to-stock and expiry must be explicit.
- **Financial policy?** Partial captures/refunds, cancellation cutoffs, refund approvals, disputes, and accounting reconciliation need agreed rules.
- **Operational target?** Expected SKU count, peak checkout throughput, staff roles, recovery objectives and launch date determine capacity tests and rollout scope.

The plan supplies safe defaults and staged options without silently selecting a multi-tenant or credit-selling business model.

## What this review cannot establish

No deployed API, TLS termination, proxy configuration, secret manager, cloud account, database grants/RLS, existing schema/data, provider dashboard, frontend token storage/rendering, or backup system was inspected. No live exploit attempts, purchase/refund calls, emails or database mutations were made. SQL injection patterns were reviewed; parameterized queries are a strength, but a full authenticated dynamic scan is still required. No direct application shell execution, arbitrary deserialization, or attacker-controlled URL-fetch path was identified in the inspected application code; this is not a universal guarantee against injection/SSRF/XSS.

Docker was installed but its daemon was unavailable, so PostgreSQL concurrency, Flyway execution, persistence/lazy-load behavior, backup restore, load testing, and network fault injection remain pending. No numerical line/branch coverage is claimed because JaCoCo was not configured/executed. The [test plan](TESTING.md) names these release gates explicitly.
