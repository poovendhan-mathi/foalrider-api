# Audit and implementation progress tracker

Updated: 2026-09-09. Baseline: `4f5cd0044b6e3872accdd96f5dade7ffd1e476bd`.

**Assessment delivered; remediation has not begun. Production status: NO-GO.** Do not mark a fix complete because its analysis or reproducer is complete.

Status vocabulary: `OPEN`, `IN_PROGRESS`, `BLOCKED`, `VERIFYING`, `DONE`. Every DONE remediation needs a PR/commit, passing test evidence, migration/rollout evidence when applicable, and reviewer. Owner labels below are suggested responsibilities; no person has been assigned.

## Assessment work

- [x] Inventory 196 Java source files, 15 controllers, 142 mappings, 24 entity tables and five migrations.
- [x] Review all module boundaries, controllers/services, repository queries, entity constraints, DTO validation and environment configuration.
- [x] Trace identity, pricing, checkout, inventory, payment/webhook/refund and invoice flows.
- [x] Run the existing suite; distinguish initial Mockito attachment failure from application test outcomes.
- [x] Build and execute opt-in HTTP/service/security acceptance probes with isolated dependencies.
- [x] Resolve dependency tree and query OSV after explicit approval; retain evidence and distinguish version matches from exploits.
- [x] Deliver findings, implementation sequence, feature roadmap, test plan and this tracker.
- [ ] Run full PostgreSQL/Flyway/concurrency/lazy-loading tests — BLOCKED: local Docker daemon unavailable; database fixture/migration repair required.
- [ ] Run authenticated DAST, provider sandbox contracts, load/soak and recovery drills — OPEN: isolated full runtime still required.
- [ ] Inspect deployment/proxy/cloud/backup/provider controls — OPEN: not present in repository audit scope.

## Remediation backlog

| ID | Priority | Work item | Suggested owner | Phase | Status | Closure evidence |
| --- | --- | --- | --- | --- | --- | --- |
| SEC-001 | Critical | Remove client payment fabrication and enforce ownership | Payments/security | 0 | OPEN | HTTP negative tests + provider reconciliation |
| SEC-002 | Critical | Rotate committed DB credential and investigate exposure | Infrastructure/security | 0 | OPEN | Owner rotation confirmation + secret scan |
| SEC-003 | High | Safe profiles/signing keys; isolate demo seeds | Platform | 0–1 | OPEN | Prod startup/seed tests |
| SEC-004 | High | Reject inactive/locked JWT principals | Identity | 0–2 | OPEN | Live-filter deactivation test |
| SEC-005 | High | Redact credentials from responses/logs | API/security | 0 | OPEN | Response + captured-log assertions |
| SEC-006 | High | JWT claims, key rotation and access-session invalidation | Identity | 2 | OPEN | Token/rotation/revocation matrix |
| SEC-007 | High | Atomic token consumption and revoke on password change | Identity | 2 | OPEN | PostgreSQL concurrent token tests |
| SEC-008 | High | Permission model/protected role assignment | Identity/admin | 2 | OPEN | Route/role matrix and last-admin tests |
| SEC-009 | Medium | Enforce catalog/review public visibility | Catalog/reviews | 2/5 | OPEN | Known-ID anonymous/owner/moderator tests |
| SEC-010 | High | Verification bootstrap and safe password policy | Identity | 2 | OPEN | Registration + byte-length encoder tests |
| SEC-011 | High | Abuse limits and trusted forwarded-IP policy | Platform/security | 2 | OPEN | Multi-instance rate/abuse tests |
| SEC-012 | High | Bound inputs, quantities, lists, sorting, nested DTOs | API | 2 | OPEN | HTTP fuzz/validation + arithmetic invariants |
| SEC-013 | Medium | Upload quotas, validation and asset lifecycle | Media/platform | 2/5 | OPEN | Multipart/provider-adapter tests |
| SEC-014 | Medium | Safe consistent error status mapping | API | 2 | OPEN | Malformed/missing/oversized input matrix |
| PAY-001 | High | Verify event identity/money and monotonic transitions | Payments | 4 | OPEN | Signed reordered/mismatched event tests |
| PAY-002 | High | Durable webhook inbox and retry semantics | Payments/platform | 4 | OPEN | DB failure + restart/replay tests |
| PAY-003 | High | Idempotent payment attempts and reconciliation | Payments | 4 | OPEN | Duplicate creation + timeout ambiguity tests |
| PAY-004 | High | Consistent refunds/cancellations/returns | Orders/payments | 3–4 | OPEN | Provider outcome + stock disposition tests |
| ORD-001 | High | Mandatory variants and explicit stock policy | Inventory/checkout | 3 | OPEN | Null/inactive/mismatched variant tests |
| ORD-002 | High | Atomic stock reservation and checkout claim | Inventory/checkout | 3 | OPEN | Last-SKU/concurrent checkout/release tests |
| ORD-003 | High | Saleability revalidation and reservation expiry | Checkout | 3 | OPEN | Inactive item + expiry/late success tests |
| ORD-004 | Medium | Durable unique order numbering | Orders | 3 | OPEN | Multi-instance/restart tests |
| PRICE-001 | High | One versioned price/currency authority | Pricing | 3 | OPEN | Cart/quote/order/intent/invoice equality |
| PRICE-002 | High | Correct inclusive tax, FX, shipping units and weight | Pricing | 3 | OPEN | Golden/property pricing cases |
| PRICE-003 | High | Exact currency minor-unit conversion | Payments/pricing | 3–4 | OPEN | Zero/2/3-decimal and overflow cases |
| DATA-001 | High | Repair fresh/upgrade migration paths | Data/platform | 1 | OPEN | Flyway + schema validation on PostgreSQL |
| DATA-002 | High | Constraints, uniqueness and safe reference lifecycle | Data/domain | 1/3/5 | OPEN | Constraint/concurrency/archive tests |
| REL-001 | High | Eliminate writes inside read-only reads | Cart/notifications | 5 | OPEN | PostgreSQL first-read/concurrent-create tests |
| REL-002 | High | Authorized invoice DTO fetch boundary | Orders | 5 | OPEN | Persisted invoice with open-in-view disabled |
| REL-003 | High | Durable notification delivery/preferences | Notifications/platform | 5 | OPEN | Outbox + provider failure/retry tests |
| REL-004 | Medium | Bounded queries and analytics | Data/performance | 5–6 | OPEN | Query budget + representative load report |
| REL-005 | Medium | Explicit Redis dependency and configuration | Platform | 1/6 | OPEN | Optional/required/TLS/outage tests |
| CAT-001 | Medium | Complete catalog edits/ancestry/archival | Catalog | 5 | OPEN | Child updates/cycles/conflicts tests |
| REV-001 | Medium | Consistent review/vote aggregates | Reviews | 5 | OPEN | Moderation transitions/concurrent votes |
| BIZ-001 | Medium | Honest coupon behavior and settled reporting | Product/reporting | 3/5 | OPEN | Redemption + ledger reconciliation |
| OPS-001 | High | CI, monitoring, audit trail, backup/recovery | Platform | 1/6 | OPEN | Pipeline + alert + restore drill evidence |
| TEST-001 | High | Enabled integration/security gates and current docs | QA/platform | 1/6 | OPEN | Clean-machine CI and mandatory probes |
| DEP-001 | High | Upgrade and triage dependency advisories | Platform/security | 1 | OPEN | Built-artifact SBOM, rescan, reachability review |

## Release gates

- [ ] G0: exposed secrets rotated; SEC-001/004/005 fixed and independently reviewed.
- [ ] G1: fresh and upgrade migrations pass; production startup tested; demo logins absent.
- [ ] G2: identity/authorization/validation/abuse matrix passes; privileged MFA/policies ready.
- [ ] G3: quote/order/payment/invoice agree; no overselling or duplicate reservation under concurrency.
- [ ] G4: payment/refund replay, reordering, timeout and crash tests converge correctly.
- [ ] G5: notifications recover; reports reconcile; durable action audit and operational dashboards work.
- [ ] G6: sustained load, backup restore, deployment recovery and independent security review complete.

## Per-task update template

```text
Finding/task:
Assigned person:
Status:
PR/commit:
Tests added/promoted to mandatory CI:
Observed test results and artifact links:
Migration/data impact:
Rollout and rollback/forward-repair evidence:
Reviewer:
Remaining limitations:
Completed date:
```

Start with phase 0 and DATA-001/TEST-001. Feature work is tracked separately in [FEATURE_ROADMAP.md](FEATURE_ROADMAP.md); do not count optional growth features as security remediation.
