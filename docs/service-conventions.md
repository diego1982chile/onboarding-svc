# Service Conventions

These conventions are intended for `onboarding-svc` and future Quarkus services
that follow the same Spring-style structure.

## Package Layout

Use technical layers under the service package prefix:

```text
cl.dsoto.<service>
  clients
  entities
  eventfeeds
  identity/events/adapters
  identity/events
  model
  repositories
  services
  services/impl
  webservice
  webservice/impl
  webservice/resources
```

Use a capability prefix when a package is specific to an upstream or business
area. For example, identity events live under:

```text
cl.dsoto.onboarding.identity.events
```

Use `clients` for outbound REST client interfaces, client-token helpers, and
payloads that belong only to external client contracts. Raw MicroProfile REST
clients should be easy to find there instead of being buried inside a domain
event package.

```text
clients/TokenAuthRestClient.java
clients/TokenIdentityEventFeedRestClient.java
clients/TokenServiceAccessTokenProvider.java
clients/resources/AccessTokenResource.java
```

Use `clients.resources` for payloads that belong only to outbound client
contracts. Prefer `*Resource` names for serialized HTTP payloads and avoid
names that make payloads look like clients.

Use capability-specific `adapters` packages when a class translates an external
transport into an application capability. For example,
`identity.events.adapters.TokenIdentityEventFeedAdapter` wraps the raw token
REST client with authorization, token renewal, one retry after `401`, and maps
the result into the identity event feed consumed by the poller.

Use `eventfeeds` for generic feed polling mechanics such as cursor loading,
cursor persistence, retry orchestration, and scheduler/poller classes.

```text
eventfeeds/EventFeedPoller.java
eventfeeds/EventFeedCursorStore.java
```

Domain-specific event payloads and handlers should remain in their capability
package. For identity:

```text
identity/events/IdentityEventFeedItem.java
identity/events/IdentityEventFeedPage.java
identity/events/IdentityEventHandler.java
identity/events/adapters/TokenIdentityEventFeedAdapter.java
```

## Web Services

Use `webservice` for exposed HTTP API contracts.

```text
webservice/OnboardingWebService.java
webservice/impl/DefaultOnboardingWebService.java
```

The interface defines the API contract. The implementation owns the Quarkus
REST annotations, security annotations, request validation annotations, and
transport-level `Response` handling.

Do not name endpoint implementations `*Resource`. Reserve `Resource` for API
payload objects.

## Web Service Resources

Use `webservice.resources` for objects serialized through HTTP APIs.

```text
webservice/resources/RegistrationStatusResource.java
webservice/resources/OnboardingTrainResource.java
webservice/resources/OnboardingTrainStepResource.java
```

Avoid generic `dto` packages. Prefer names that describe the role in the API
contract. In this convention, `Resource` means an external API representation.

Enums that are part of the API contract may also live in
`webservice.resources`.

## Services

Use interfaces in `services` and implementations in `services.impl`.

```text
services/OnboardingEngine.java
services/impl/DefaultOnboardingEngine.java
```

Implementation classes should use the `Default` prefix unless there is a more
specific implementation name.

Services own application behavior. They may orchestrate repositories, domain
events, rules, and external adapters, but should not expose persistence entities
as HTTP API payloads.

## Entities

Use `entities` for JPA persistence classes and suffix every entity class with
`Entity`.

```text
entities/OnboardingProcessEntity.java
entities/ProcessedIdentityEventEntity.java
```

The class suffix makes persistence objects explicit and keeps them distinct
from domain/application models and HTTP resources.

Entity table names should remain stable even if Java class names change.

## Repositories

Use `repositories` for Spring Data JPA repositories.

```text
repositories/OnboardingProcessRepository.java
```

Repository names should describe the aggregate or persistence record they query,
without repeating the `Entity` suffix.

```java
public interface OnboardingProcessRepository
        extends JpaRepository<OnboardingProcessEntity, String> {
}
```

## Model

Use `model` for application concepts that are not persistence records and not
HTTP-specific payloads.

```text
model/OnboardingEvent.java
model/OnboardingEventType.java
model/OnboardingState.java
```

Do not create a model class only to mirror an entity. Add a model class when it
represents a real application concept, hides persistence details, or prevents
HTTP/API contracts from depending on database records.

## Events And Adapters

Use capability-specific packages for integration contracts and adapters.

```text
identity/events
identity/events/sqs
```

Do not put all feed infrastructure under `identity.events`. The poll/cursor
mechanics are shared infrastructure. Identity-specific feed items, mappers,
handlers, and adapters remain under `identity.events`.

Event envelope classes should stay close to the adapter or capability they
belong to. If a class is only needed by an SNS/SQS prototype, keep it under the
identity event integration instead of promoting it to the generic model.

Only explicit user-facing flows should emit identity events. Administrative,
support, migration, and internal maintenance actions should not emit onboarding
events by default.

## Tests

Mirror the production package structure in tests.

```text
src/test/java/cl/dsoto/onboarding/webservice/impl
src/test/java/cl/dsoto/onboarding/services
src/test/java/cl/dsoto/onboarding/identity/events
```

Use `@QuarkusTest` for service and webservice behavior tests, and keep assertions
focused on externally observable behavior or service contracts.
