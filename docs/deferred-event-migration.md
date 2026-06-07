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
  -> handles each IdentityEventFeedItem through IdentityEventHandler
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
  -> TokenIdentityEventFeedAdapter
  -> IdentityEventHandler
  -> IdentityEventMapper
  -> OnboardingEngine
```

Responsibilities:

- `EventFeedPoller` owns polling cadence, cursor loading, cursor persistence,
  and retry behavior. It should stay generic enough to avoid creating one
  poller class per future domain.
- `TokenIdentityEventFeedAdapter` adapts the identity feed exposed by
  `token-svc` into the application flow. It uses the raw token REST client,
  adds the technical JWT, renews the cached token after `401`, retries once,
  and returns `IdentityEventFeedPage` / `IdentityEventFeedItem` values.
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

The HTTP feed path now uses `IdentityEventFeedItem` directly. The older
`IdentityEventEnvelope` remains tied to the SNS/SQS prototype and should not be
promoted as the central HTTP feed contract.

Avoid duplicating the whole polling stack for every future integration. The
poll/cursor mechanics are generic infrastructure; event contracts and handlers
remain domain-specific.

Expected future shape:

```text
feed/EventFeedPoller
  -> TokenIdentityEventFeedAdapter
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
- The HTTP feed path was smoke-tested locally with both services running:
  public registration in `token-svc`, email confirmation in `token-svc`, feed
  polling in `onboarding-svc`, and onboarding status advancing to
  `EMAIL_VERIFIED` / `IDENTITY_CHECK`.
- Feed clients and polling classes were reorganized so outbound clients live in
  `clients`, generic polling/cursor infrastructure lives in `eventfeeds`, and
  identity feed payloads remain in `identity.events`.
- Automated feed idempotency coverage was added: when the feed returns an
  already processed event after a cursor reset/rollback, `IdentityEventHandler`
  ignores it by `eventId` and `EventFeedPoller` still advances the cursor.
- Automated token-renewal coverage was added: cached tokens are refreshed when
  they are inside the configured skew window, the feed adapter invalidates the
  cache and retries once after `401`, and non-`401` errors are not retried.
- The temporary local onboarding state handling was removed from `token-svc`:
  local `OnboardingProcess`, local engine, Easy Rules transitions, repository,
  and `DefaultUserService` calls were deleted. `token-svc` keeps only identity
  event log persistence, selective event emission, and the protected feed.
- The test suite passes with 44 tests.

Next checkpoint:

- Run a final cross-service smoke test after the removal: registration and email
  confirmation in `token-svc`, feed polling in `onboarding-svc`, and onboarding
  state updated from the feed.

## Next Implementation Checklist

1. Optionally run a manual duplicate-delivery smoke test by resetting the
   stored HTTP feed cursor and confirming the same behavior against a live
   database.
2. Optionally run a manual token-renewal smoke test by forcing an invalid
   cached service JWT and confirming one successful retry against live
   `token-svc`.
3. Run a final cross-service smoke test after removing local onboarding from
   `token-svc`.
4. Keep `token-svc` event emission selective: public registration and email
   confirmation flows emit identity events; administrative/support/migration
   actions do not emit onboarding events by default.
5. Revisit REST endpoint naming/alignment only for permanent API surfaces.

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

- Manual duplicate-delivery smoke verification by resetting the HTTP feed
  cursor in a live database.
- Manual token-renewal smoke verification after forcing an expired/invalid
  cached service JWT.
- Transactional outbox publishing to SNS.
- Camel Quarkus routes.
- Removal of onboarding code from `token-svc`.

Direct REST service-to-service event delivery is not planned. The planned HTTP
approach is an incremental event feed with cursor and idempotency, not a
synchronous callback from `token-svc` to `onboarding-svc`.
