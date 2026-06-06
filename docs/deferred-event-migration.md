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

## Target Architecture

When the migration is resumed, identity changes will be distributed
asynchronously:

```text
token-svc
  -> transactional outbox
  -> SNS topic: identity-events
  -> SQS queue: onboarding-identity-events
  -> onboarding-svc

SQS queue: onboarding-identity-events
  -> DLQ: onboarding-identity-events-dlq
```

Local development will use LocalStack for SNS and SQS.

Camel Quarkus is not part of the initial migration. It should be reconsidered
when KYC, Stripe, or other integrations create multiple routes that benefit
from shared transformation, routing, or error-handling behavior.

## Event Contract

Events must use a versioned, transport-neutral envelope:

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

Consumers must be idempotent because standard SNS and SQS delivery can produce
duplicate messages. Processed `eventId` values must be persisted separately
from state-transition idempotency.

## Consumer Design Pattern

The SQS consumer must be implemented as an infrastructure adapter, not as part
of the onboarding domain model.

The intended internal flow is:

```text
SqsIdentityEventConsumer
  -> IdentityEventHandler
  -> IdentityEventMapper
  -> OnboardingEngine
```

Responsibilities:

- `SqsIdentityEventConsumer` owns AWS/SQS concerns: receiving messages,
  extracting the SNS payload from the SQS body, acknowledging successful
  processing by deleting messages, and leaving failed messages available for
  retry/DLQ handling.
- `IdentityEventHandler` owns application coordination: idempotency by
  `eventId`, mapping the external identity event, and invoking the onboarding
  engine inside the correct transaction boundary.
- `IdentityEventMapper` converts the external `IdentityEventEnvelope` into the
  internal `OnboardingEvent`.
- `OnboardingEngine` owns only onboarding business rules and state
  transitions. It must not depend on AWS SDK, SNS, SQS, JSON transport details,
  or queue retry behavior.

This keeps the migration close to a small ports-and-adapters design without
adding unnecessary abstractions. If the transport changes later, only the
infrastructure adapter should change.

## Migration Order

1. Move the onboarding domain model, persistence, Easy Rules engine, train
   projection, and tests from `token-svc` to `onboarding-svc`.
2. Define and test the versioned external event contract.
3. Add LocalStack and reproducible SNS, SQS, subscription, and DLQ
   provisioning.
4. Add the SQS consumer in `onboarding-svc`.
5. Add a transactional outbox and SNS publisher in `token-svc`.
6. Replace local `OnboardingEngine.applyEvent(...)` calls in `token-svc` with
   outbox events.
7. Build all onboarding train views from `onboarding-svc` persistence.
8. Remove onboarding entities, rules, repositories, endpoints, and tables from
   `token-svc`.
9. Verify registration, email confirmation, duplicate delivery, retries,
   service downtime, and DLQ behavior.

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
- The test suite passes with 35 tests.

Next checkpoint:

- Add a transactional outbox and SNS publisher in `token-svc`.
- Replace direct `OnboardingEngine.applyEvent(...)` calls in `token-svc` with
  outbox event persistence.
- Publish outbox events to SNS topic `identity-events`.

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

- Transactional outbox publishing.
- End-to-end cross-service event flow from `token-svc`.
- Camel Quarkus routes.
- Removal of onboarding code from `token-svc`.

REST service-to-service event delivery is not planned as an intermediate step.
It would require B2B authentication, retry handling, and availability coupling
without providing the reliability benefits of SQS.
