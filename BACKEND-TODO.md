# Backend TODO — Priority Tasks

Date: 13 January 2026

Purpose: short, actionable list to continue backend development (implement features, tests, infra wiring). This complements the overall project plans in `Documents/`.

Checklist (requirements extracted from discussion):

- [x] Proceed with backend development first
- [x] Keep account setup for tomorrow (Documented in `Documents/REMINDERS.md`)
- [ ] Implement email & notification plumbing (SendGrid integration stubbed for now)
- [ ] Add email verification and password reset flows
- [ ] Complete payment-related integration tests (Stripe webhook handling)
- [ ] Add unit/integration tests for auth and user flows
- [ ] Add CI steps for builds and tests

Priority 1 — Auth & Email

- Implement `EmailService` interface and a `SendGridEmailService` implementation (config + stubbed client when API key not present).
  - Files to add/modify:
    - `com.foalrider.notification.service.EmailService` (new)
    - `com.foalrider.notification.service.SendGridEmailService` (new)
    - Register bean in config (e.g., `NotificationConfig`)
- Email verification flow:
  - Generate token on register (AuthServiceImpl) -> currently `User` entity has `emailVerificationToken` + `emailVerificationTokenExpiresAt` fields.
  - Endpoint: `POST /auth/verify-email?token=...` (AuthController)
  - Use `UserRepository.findByEmailVerificationToken(token)` and mark `isEmailVerified = true`.
- Password reset flow:
  - Endpoint: `POST /auth/forgot-password` (send email with reset token)
  - Endpoint: `POST /auth/reset-password` (accept token + new password)
  - Persist reset token on `User` (reuse `emailVerificationToken` or add `passwordResetToken` fields) — prefer separate field.

Priority 2 — Payments & Webhooks

- Ensure `WebhookController` is idempotent and validated (Stripe signature). Add unit tests for `PaymentServiceImpl.handleWebhookEvent` handling `payment_intent.succeeded` and `payment_intent.payment_failed`.
- Add integration test that posts sample webhook payloads using Stripe test fixtures.

Priority 3 — Tests, CI, Docs

- Add unit tests for `AuthServiceImpl` (register, login, refresh, logout)
- Add tests for `UserServiceImpl` (change password, update profile)
- Add CI job in repository (GitHub Actions or Railway) to run `mvn -DskipTests=false test` and `mvn -DskipTests=false verify`.
- Update `Documents/` with short dev notes (where to put env vars: `SPRING_DATASOURCE_*`, `STRIPE_API_KEY`, `STRIPE_WEBHOOK_SECRET`, `SENDGRID_API_KEY`).

Mapping to existing code (useful starting points):

- AuthServiceImpl: `src/main/java/com/foalrider/modules/auth/service/AuthServiceImpl.java` — register/login/refresh implemented, no verify/forgot/reset yet
- User entity/repository: `.../modules/user/entity/User.java`, `.../modules/user/repository/UserRepository.java` — token fields present
- Notification service: `.../modules/notification/service/NotificationServiceImpl.java` — preference handling present, but EmailService not found by grep
- PaymentServiceImpl & WebhookController: `.../modules/payment/service/PaymentServiceImpl.java`, `.../modules/payment/controller/WebhookController.java`

Assumptions:

- We will implement email sending as a pluggable service so actual account/API keys are optional during development. Tests can mock the email client.
- The database schema already contains fields for basic verification tokens. If not, we will add a small migration later.

Immediate next steps I can take now (pick one or more):

1. Create the `EmailService` interface and a `SendGridEmailService` stub + config bean.
2. Implement `POST /auth/forgot-password` and `POST /auth/reset-password` endpoints and service methods (token generation + validation).
3. Add unit tests for the new auth flows.

Which immediate step would you like me to start with? If no preference, I'll implement the `EmailService` + SendGrid stub and wire it into the notification/auth flows (safe, non-blocking change).
