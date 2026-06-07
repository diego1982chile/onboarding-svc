# Deferred Onboarding Event Migration

## Decision

The migration of onboarding state ownership from `token-svc` to
`onboarding-svc` is intentionally deferred.

The current implementation is sufficient to continue product development:

- `token-svc` owns registration, email confirmation, credentials, and JWT
  issuance.
- `onboarding-svc` owns the client-facing onboarding train.
- Public train endpoints remain anonymous.
- The authenticated train endpoint validates JWTs issued by `token-svc`.

The existing onboarding state machine in `token-svc` must not be redesigned
from scratch. It already contains reusable domain events, persistence,
Easy Rules transitions, train projection logic, and tests.

## Current Direction

The SNS/SQS path was implemented and smoke-tested locally, but it is not the
immediate target for the next migration step. For the current product scope, it
adds more infrastructure than the first identity-only event flow needs.

The immediate target is an incremental HTTP event feed:

```text
token-svc
  -> identity event log
  -> GET /internal/identity-events?after=<cursor>&limit=<n>

onboarding-svc
  -> polls the identity event feed
  -> persists its source cursor
  -> handles each IdentityEventEnvelope through IdentityEventHandler
```

The HTTP feed must be cursor-based. `onboarding-svc` must not poll current user
state or infer changes by comparing snapshots.

The SNS/SQS implementation remains a validated prototype and future option if
fan-out, durable queueing, DLQ operations, or multiple independent consumers
justify the extra infrastructure.

KYC is expected to be an external provider integration owned by
`onboarding-svc`, not by `token-svc`. KYC callbacks/webhooks should be
translated inside `onboarding-svc` into onboarding events such as
`KYC_APPROVED` or `KYC_REJECTED`.

Subscription plan and billing ownership is separate. `onboarding-svc` may own a
temporary plan-selection catalog while the product is simple, but payment
provider callbacks should enter the service that owns subscriptions. If a
future `subscription-svc` exists, `onboarding-svc` should consume subscription
outcomes from that service rather than handling payment provider webhooks
directly.

Camel Quarkus is not part of the initial migration. It should be reconsidered
when KYC, Stripe, or other integrations create multiple routes that benefit
from shared transformation, routing, or error-handling behavior.

## Event Contract

The immediate HTTP feed contract must model a cursor-based event log, not a
message-queue envelope.

`token-svc` should expose pages shaped around feed items:

```json
{
  "events": [
    {
      "sequence": 126,
      "eventId": "uuid",
      "eventType": "USER_REGISTERED",
      "subject": "user@example.com",
      "occurredAt": "2026-06-03T12:00:00Z",
      "registrationId": "uuid"
    }
  ],
  "nextCursor": 126
}
```

The intended model names for the HTTP feed are:

- `IdentityEventFeedPage`
- `IdentityEventFeedItem`

`IdentityEventFeedItem.sequence` is the cursor position. `eventId` is still
required for idempotent processing.

The existing `IdentityEventEnvelope` belongs to the validated SNS/SQS
prototype. It is not the right central contract for the immediate HTTP feed.
It may remain as an SQS/future-transport message shape, but the HTTP feed
should not be forced to wrap events in it.

SQS prototype envelope:

```json
{
  "version": 1,
  "eventId": "uuid",
  "eventType": "USER_REGISTERED",
  "subject": "user@example.com",
  "occurredAt": "2026-06-03T12:00:00Z",
  "registrationId": "uuid"
}
```

Initial event types:

- `USER_REGISTERED`
- `EMAIL_VERIFIED`

Future event types may include KYC, profile, plan, and subscription outcomes.

Consumers must be idempotent because feed polling can retry pages and future
queue-based transports can produce duplicate messages. Processed `eventId`
values must be persisted separately from state-transition idempotency.

## Consumer Design Pattern

Event consumption must be implemented as an infrastructure adapter, not as part
of the onboarding domain model.

The intended immediate flow is:

```text
EventFeedPoller
  -> TokenIdentityEventFeedClient
  -> IdentityEventHandler
  -> IdentityEventMapper
  -> OnboardingEngine
```

Responsibilities:

- `EventFeedPoller` owns polling cadence, cursor loading, cursor persistence,
  and retry behavior. It should stay generic enough to avoid creating one
  poller class per future domain.
- `TokenIdentityEventFeedClient` owns HTTP calls to `token-svc` and maps the
  feed response into `IdentityEventFeedPage` / `IdentityEventFeedItem` values.
- `IdentityEventHandler` owns application coordination: idempotency by
  `eventId`, mapping the external identity event, and invoking the onboarding
  engine inside the correct transaction boundary.
- `IdentityEventMapper` converts the external feed item into the
  internal `OnboardingEvent`.
- `OnboardingEngine` owns only onboarding business rules and state
  transitions. It must not depend on HTTP clients, AWS SDK, SNS, SQS, JSON
  transport details, cursor handling, or retry behavior.

This keeps the migration close to a small ports-and-adapters design without
adding unnecessary abstractions. If the transport changes later, only the
infrastructure adapter should change.

The validated SNS/SQS adapter follows the same boundary:

```text
SqsIdentityEventConsumer
  -> IdentityEventEnvelope
  -> IdentityEventHandler
  -> IdentityEventMapper
  -> OnboardingEngine
```

Before implementing the HTTP feed poller, review the current
`IdentityEventHandler` and `IdentityEventMapper` signatures. They currently
accept `IdentityEventEnvelope` because the SQS prototype was implemented
first. For the HTTP feed direction, they should either accept
`IdentityEventFeedItem` directly or use a neutral application command that both
feed items and SQS envelopes can map into.

Avoid duplicating the whole polling stack for every future integration. The
poll/cursor mechanics are generic infrastructure; event contracts and handlers
remain domain-specific.

Expected future shape:

```text
feed/EventFeedPoller
  -> TokenIdentityEventFeedClient
  -> IdentityEventHandler

webhook/KycWebhookResource
  -> KycEventHandler

feed/EventFeedPoller
  -> SubscriptionEventFeedClient
  -> SubscriptionEventHandler
```

KYC provider callbacks are expected to enter `onboarding-svc` directly. Billing
provider callbacks should enter whichever service owns subscriptions. If that
is a future `subscription-svc`, `onboarding-svc` can reuse the generic
poll/cursor infrastructure with a subscription-specific feed client and
handler, without duplicating the polling stack.

## Migration Order

1. Move the onboarding domain model, persistence, Easy Rules engine, train
   projection, and tests from `token-svc` to `onboarding-svc`.
2. Define and test the versioned external event contract.
3. Add application-level event handling and idempotency by `eventId`.
4. Add an identity event log in `token-svc`.
5. Expose a cursor-based internal identity event feed in `token-svc`.
6. Add an HTTP feed poller in `onboarding-svc`.
7. Replace local `OnboardingEngine.applyEvent(...)` calls in `token-svc` with
   identity event log persistence.
8. Build all onboarding train views from `onboarding-svc` persistence.
9. Remove onboarding entities, rules, repositories, endpoints, and tables from
   `token-svc`.
10. Verify registration, email confirmation, duplicate delivery, retries,
   service downtime, cursor recovery, and idempotency.

## Current Migration Status

Completed:

- Reusable onboarding domain, persistence, Easy Rules transitions, train
  projection, and tests were moved into `onboarding-svc`.
- The identity event envelope contract was added and tested through
  `IdentityEventEnvelope` and `IdentityEventMapper`.
- Application-level identity event handling was added through
  `IdentityEventHandler`, including idempotency by persisted `eventId`.
- The SQS infrastructure adapter was added through `SqsIdentityEventConsumer`.
  It parses SNS-wrapped SQS messages, delegates to `IdentityEventHandler`, and
  deletes messages only after successful processing.
- Local event infrastructure was added for LocalStack, SNS, SQS, subscription,
  and DLQ provisioning.
- LocalStack provisioning was executed successfully against a live container.
- A local smoke test published sample `USER_REGISTERED` and `EMAIL_VERIFIED`
  events to SNS, consumed them from SQS, and verified the resulting onboarding
  status over HTTP.
- The SNS/SQS path is now treated as a validated prototype and future option,
  not the immediate migration target.
- `token-svc` now persists identity events in an internal event log and exposes
  a cursor-based feed at `GET /internal/identity-events?after=<cursor>&limit=<n>`.
- The identity event feed is protected for service clients through the
  `token.identity-events.read` scope.
- `token-svc` exposes a client-credentials endpoint for `onboarding-svc` to
  obtain a technical JWT for the feed.
- `onboarding-svc` now has Quarkus REST clients for token acquisition and
  identity event feed consumption.
- `onboarding-svc` caches the technical JWT, renews it before expiration, and
  retries the feed once after a `401`.
- `onboarding-svc` now has an HTTP feed poller that reads pages, delegates each
  event to `IdentityEventHandler`, and advances the persisted cursor only after
  an item is processed.
- The test suite passes with 40 tests.

Next checkpoint:

- Run an end-to-end local smoke test with both services running:
  registration in `token-svc`, feed polling in `onboarding-svc`, and onboarding
  train state updated from the feed.
- Once the feed path is verified end to end, remove the temporary local
  onboarding engine calls and onboarding persistence from `token-svc`.

## Next Implementation Checklist

1. Run the HTTP feed smoke test locally with `identity.events.feed.enabled=true`.
2. Verify duplicate delivery by resetting the stored cursor and confirming
   idempotency through `ProcessedIdentityEventEntity`.
3. Verify token renewal by forcing an expired/invalid cached token and
   confirming one successful retry.
4. Remove local onboarding state handling from `token-svc`:
   `OnboardingProcess`, local engine, rules, repository, and temporary
   `DefaultUserService` calls.
5. Keep `token-svc` event emission selective: public registration and email
   confirmation flows emit identity events; administrative/support/migration
   actions do not emit onboarding events by default.
6. After local onboarding is removed from `token-svc`, revisit REST endpoint
   naming/alignment only for permanent API surfaces.

Identity events from `token-svc` must be selective. The feed is not a general
user audit log. Only explicit user-facing onboarding flows should append
events, initially public registration (`USER_REGISTERED`) and user email
confirmation (`EMAIL_VERIFIED`). Administrative user changes must not emit
onboarding identity events by default.

## Current Local Event Infrastructure

Local SNS/SQS provisioning is available through:

```sh
docker compose up -d localstack
sh scripts/provision-localstack.sh
```

The script provisions:

- SNS topic: `identity-events`
- SQS queue: `onboarding-identity-events`
- DLQ: `onboarding-identity-events-dlq`
- SNS subscription from `identity-events` to `onboarding-identity-events`
- SQS redrive policy to the DLQ

## Explicitly Deferred

The following are not implemented yet:

- End-to-end cross-service event flow from `token-svc`.
- Transactional outbox publishing to SNS.
- Camel Quarkus routes.
- Removal of onboarding code from `token-svc`.

Direct REST service-to-service event delivery is not planned. The planned HTTP
approach is an incremental event feed with cursor and idempotency, not a
synchronous callback from `token-svc` to `onboarding-svc`.
