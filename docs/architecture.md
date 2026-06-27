# Onboarding Service Architecture

## Responsibility

`onboarding-svc` owns the product onboarding flow and the client-facing train.
It composes progress from services that own individual business capabilities.

Owned by this service:

- Visible onboarding steps, labels, ordering, and statuses.
- Mapping detailed capability states into the product onboarding train.
- Public and authenticated onboarding views.
- Future onboarding state transitions triggered by initial profile creation
  results.
- Authorization of its API endpoints after validating access tokens issued by
  `token-svc`.

Not owned by this service:

- User credentials, login, JWT issuance, or account activation.
- Email confirmation token validation.
- Access token issuance, signing keys, or credential validation.
- Profile data.
- Plans, checkout, billing, or subscription state.

## Initial Integration

`token-svc` is temporarily the identity service and remains the source of truth
for anonymous registration and email confirmation.

```text
UI -> onboarding-svc -> token-svc
```

`onboarding-svc` uses only the `confirmed` value returned by `token-svc` for a
registration id. It does not reuse the train returned by the transitional
`token-svc` endpoint.

## Initial API

Base path:

```text
/onboarding-service/api
```

Endpoints:

```http
POST /onboarding/start
GET /onboarding/train
GET /onboarding/registrations/{registrationId}/status
GET /onboarding/me/train
```

The start endpoint is the email-first resume entry point for the UI. It reads
only `onboarding-svc` local projection state populated by events from source
services. It must not perform a synchronous identity lookup in `token-svc`.

The anonymous train always starts at `REGISTRATION`. Query parameters such as
`stage=email-confirmed` are not trusted to advance onboarding state.

The registration status endpoint returns `404` when the identity service does
not know the registration id.

The `/train` and `/registrations/{registrationId}/status` endpoints are
anonymous and explicitly use `@PermitAll`. `/me/train` requires `USER` or
`ADMIN`, validates the JWT against the JWKS
published by `token-svc`, and uses the `sub` claim as the temporary user
identifier.

## JWT Trust

```text
token-svc private key -> signs access JWT
token-svc JWKS endpoint -> distributes public key
onboarding-svc -> validates issuer, audience, signature, expiration, and roles
```

Profile integration remains deferred. Subscription, payment, media upload,
profile publication, and publication-readiness checks are outside account
onboarding. Age verification is not part of account onboarding; see
[ADR-012 - Age Verification](adr-012-age-verification.md).

## Onboarding State Machine Direction

The onboarding train must not be treated as a fixed Java `switch` plus enum
ordinal ordering. That approach already became misleading when age verification
was moved out of onboarding.

For the MVP, the desired direction is a simple declarative flow definition
versioned in code, not a database-editable workflow engine.

The flow definition should own:

- Visible steps.
- Step labels.
- Step ordering.
- The event or fact that completes each step.
- How current, completed, and pending statuses are projected.

The user process should persist durable onboarding facts or states, while the
train projection should derive the visible step status from the flow definition.

Age verification must not be modeled as an onboarding step or state.
Subscription selection must not be modeled as an onboarding step. Publication
eligibility belongs outside onboarding and should be evaluated by the publishing
domain using profile, subscription, and age-verification information.

This is intentionally not a decision to introduce a full workflow engine or
database-configurable state machine for the MVP.

## Deferred State Ownership Migration

`token-svc` still contains the existing onboarding persistence and Easy Rules
state machine. That implementation is reusable and will eventually move to
`onboarding-svc`; it must not be independently reimplemented here.

The immediate migration path uses a cursor-based HTTP identity event feed from
`token-svc`, consumed by `onboarding-svc` through Quarkus REST clients and a
poller that persists its cursor. SNS/SQS was validated locally with LocalStack,
but remains a future option instead of the initial production path.

The feed is protected by a technical JWT. `onboarding-svc` obtains that token
through client credentials, caches it until it is close to expiration, and
retries once when the feed returns `401`.

See [Deferred Onboarding Event Migration](deferred-event-migration.md) for the
target architecture, event contract, migration order, and explicitly deferred
work.
