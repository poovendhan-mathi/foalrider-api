# Backend security and stability audit

Audit date: 2026-09-09. Baseline commit: `4f5cd0044b6e3872accdd96f5dade7ffd1e476bd`.

**Release recommendation: NO-GO for real customer payments until the critical findings and release gates below are resolved.** This is an assessment and implementation roadmap; it does not claim the backend has been hardened. Production application code is unchanged by this audit.

## Read in this order

1. [Findings and evidence](FINDINGS.md): security, financial correctness, reliability, and maintainability defects.
2. [Implementation plan](IMPLEMENTATION_PLAN.md): ordered work packages, dependencies, acceptance criteria, and rollout approach.
3. [Progress tracker](PROGRESS_TRACKER.md): assessment progress and outstanding remediation, kept separate.
4. [Testing and release gates](TESTING.md): executed checks, reproducible probes, and remaining integration/security tests.
5. [Business feature roadmap](FEATURE_ROADMAP.md): necessary commerce capabilities and conditional B2B/multi-business requirements.
6. [Architecture and coverage](ARCHITECTURE.md): module assessment, trust boundaries, strengths, and scope limits.

## Immediate priorities

| Order | Work | Why |
| --- | --- | --- |
| 1 | Rotate exposed database credentials; remove committed secrets and unsafe environment defaults | Source contains a database password and reusable development signing material |
| 2 | Remove client authority to mark orders paid | Any authenticated caller can invoke payment confirmation without server-side Stripe verification |
| 3 | Enforce disabled-account checks and session invalidation | Existing JWTs bypass account-state checks in the custom filter |
| 4 | Repair migrations and provide an isolated PostgreSQL test environment | The migration chain cannot recreate the entity model |
| 5 | Centralize pricing and transactional inventory reservation | Cart, checkout, and payment use incompatible assumptions |
| 6 | Add durable payment event processing and refund reconciliation | Replays, stale events, and partial failures can corrupt business state |
| 7 | Enforce the security and operational release gates | Existing unit-test success is not evidence of production readiness |

## Scope and assumptions

The repository implements a Spring Boot modular monolith for one clothing retailer. It does not establish a multi-tenant business platform. B2B accounts, vendor marketplaces, and isolated businesses are proposed options, not assumed requirements. The frontend, deployed infrastructure, production database, provider dashboards, and real credentials were not tested. No live purchases, refunds, customer emails, remote database changes, or deployments were performed.

The new `*AuditProbe` classes are opt-in tests asserting **desired secure behavior**. Their failures identify open issues. They intentionally do not match Maven's normal `*Test` naming convention; remediation must promote the applicable checks into mandatory CI tests. Read the testing document before interpreting a green baseline build.
