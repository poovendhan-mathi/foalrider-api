# Business feature and enhancement roadmap

These are proposed capabilities inferred from the backend, not requirements already approved by the business. Fix the launch blockers in [the implementation plan](IMPLEMENTATION_PLAN.md) first. Build the single-retailer foundation before enabling multi-business or credit-based purchasing.

## Required for a dependable commerce launch

| Capability | Current state | Concrete increment | Acceptance criteria |
| --- | --- | --- | --- |
| Inventory administration | Variant stock fields exist; no dedicated audited adjustment workflow | Stock movements/reservations; adjustment command with reason and actor; low-stock queue | No negative stock; every adjustment attributable; checkout and manual changes cannot lose updates |
| Complete catalog editing | Product create supports children; update ignores image/variant lists | Explicit variant/image CRUD, primary-image selection, archive and restore, version conflicts | Variant changes preserve historical order snapshots; duplicate SKU conflicts are safe |
| Authoritative checkout | Separate cart/order/payment calculations | Versioned quote with currency, destination, variants, taxes, shipping and expiry | Customer-approved quote equals charged amount and invoice; stale quote handled explicitly |
| Payment/refund operations | Single intent field and incomplete status handling | Payment attempt ledger, refunds, reconciliation queue, operator replay | Every settled/returned amount ties to provider evidence; retries have one business effect |
| Fulfillment | Basic status and tracking-number fields | Shipment records, carrier/tracking events, partial shipment readiness, packing workflow | Unpaid orders cannot ship; delivered status has provenance; access scoped to authorized staff |
| Returns/exchanges | No return case or return-item model | Return authorization, reason, item quantity, inspection, refund/restock disposition | Cannot return more than purchased; no automatic restock for damaged/nonreturned items |
| Transactional notifications | Email/helper methods and preference storage exist | Outbox delivery for welcome, payment, shipping, cancellation, refunds and review decisions | Retried after outage; no duplicate action; preferences enforced; security notices cannot be suppressed by marketing opt-out |
| Customer account recovery | Reset/verify flows partially present | Initial verification, session list/revoke, recovery notifications, consistent password rules | Account compromise response invalidates affected sessions; reset links consume once |
| Staff access and action history | Coarse inconsistent role checks | Permission matrix for support, warehouse, catalog, finance and admin; privileged MFA | Support cannot alter roles/refunds beyond policy; all privileged actions logged |
| Business reporting | Several fake/inaccurate metrics | Settled gross/net revenue, refunds, taxes, shipping, inventory aging and reconciliation | Totals reconcile by currency to ledger; no unpaid order reported as revenue |
| Customer data lifecycle | Hard deletes and no defined retention | Export, anonymization/archival workflows and explicit retained transaction records | Account removal does not destroy order integrity; access/export ownership tested |

Suggested new domain tables: `inventory_movements`, `stock_reservations`, `checkout_quotes`, `payment_attempts`, `payment_events`, `refunds`, `shipments`, `shipment_items`, `returns`, `return_items`, `outbox_events`, and `audit_events`. Introduce these incrementally with indexes/constraints and migration tests. Names are suggestions, not schema commitments.

Suggested endpoint groups: `/inventory/adjustments`, `/checkout/quotes`, ownership-scoped `/orders/{id}/payments`, staff-scoped `/orders/{id}/shipments`, `/orders/{id}/returns`, `/refunds`, and `/users/me/sessions`. Define OpenAPI contracts and authorization before implementation; do not automatically expose every ledger table as CRUD.

## Growth features after correctness and recovery gates

| Priority | Feature | Design requirements |
| --- | --- | --- |
| Next | Coupons/promotions | Active dates, eligibility, minimum spend, stacking rules, per-user/global redemption limits, atomic redemption reservation, refund policy |
| Next | Wishlist and back-in-stock notifications | Ownership, product visibility, deduplication, explicit subscriptions, availability-driven events |
| Next | Better catalog discovery | Faceted search, deterministic sort, bounded pages, category/brand filters, sensible unavailable-item handling; measure SQL before adding a search engine |
| Next | Guest checkout if commercially needed | Scoped signed order-access token, recovery flow, PII minimization, abuse limits, safe account linking; email knowledge alone must not reveal an order |
| Later | Bulk import/export | Dry run, row validation, resumable jobs, bounded files, authorization, spreadsheet formula-injection defenses, reversible changes and audit history |
| Later | Multiwarehouse and purchasing | Warehouse-level stock, transfers, receiving, supplier purchase orders, reorder policies and allocation rules |
| Later | Loyalty/store credit/gift cards | Immutable balance ledger, atomic spend, expiry policy, refund semantics, fraud limits and reconciliation; never store only a mutable balance |
| Later | Advanced analytics and recommendations | Settled event data, consent/retention, explicit freshness and reproducible metric definitions |

## If “for businesses” means B2B buyers

Add company accounts and memberships with buyer/approver/billing roles. Support negotiated price lists, quantity breaks, minimum order quantities, quote requests, purchase-order references, approval thresholds and business invoicing. Treat net payment terms and credit limits as a separate financial product requiring explicit policy and operational ownership.

Acceptance tests must cover cross-company access, removed members, stale price lists, approval bypass, self-approval, changed quote amounts, duplicated purchase orders, and overspending a credit limit concurrently. Keep personal and company purchases distinguishable. Do not launch credit terms merely by adding a `paymentStatus` enum value.

## If the platform will host multiple independent businesses

This is a material architecture change. Current entities and repositories have no tenant ownership boundary. Add a business/tenant model, memberships, tenant-scoped uniqueness and ownership across catalog, orders, assets, reports, cache keys, jobs, notifications, audit records and provider configuration. Decide merchant-of-record and payment-account routing before implementation.

Use defense-in-depth tenant isolation in repository predicates and database policy where appropriate, with a global privileged-access model that is explicit and audited. Add a mandatory tenant-A/tenant-B test matrix for every route and background task. A request tenant ID is untrusted and must be validated against membership. VENDOR role alone does not isolate sellers or qualify this code as a marketplace.

## Business decisions to record before feature implementation

1. Single retailer, B2B company purchasing, vendor marketplace, or isolated business tenants.
2. Supported currencies, countries, shipping services, tax calculation owner and invoice requirements.
3. Cancellation/return windows, partial refunds, damaged-return rules, backorders and reservation duration.
4. Staff permissions, refund approval limits, MFA requirements and emergency access process.
5. Peak traffic/order volume, SKU count, uptime/recovery targets, launch scope and budget.

The implementation sequence does not depend on choosing every growth feature now. Keep unsupported behaviors rejected or clearly unavailable while the core transaction system is hardened.
