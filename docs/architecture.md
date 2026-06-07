# Onboarding Service Architecture

## Responsibility

`onboarding-svc` owns the product onboarding flow and the client-facing train.
It composes progress from services that own individual business capabilities.

Owned by this service:

- Visible onboarding steps, labels, ordering, and statuses.
- Mapping detailed capability states into the product onboarding train.
- Public and authenticated onboarding views.
- Future onboarding state transitions triggered by KYC, profile, and subscription
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
GET /onboarding/public/train
GET /onboarding/public/{registrationId}/status
GET /onboarding/me/train
```

The public train always starts at `REGISTRATION`. Query parameters such as
`stage=email-confirmed` are not trusted to advance onboarding state.

The registration status endpoint returns `404` when the identity service does
not know the registration id.

The two `/public` endpoints are anonymous and explicitly use `@PermitAll`.
`/me/train` requires `USER` or `ADMIN`, validates the JWT against the JWKS
published by `token-svc`, and uses the `sub` claim as the temporary user
identifier.

## JWT Trust

```text
token-svc private key -> signs access JWT
token-svc JWKS endpoint -> distributes public key
onboarding-svc -> validates issuer, audience, signature, expiration, and roles
```

KYC, profile, and subscription integrations remain deferred.

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
