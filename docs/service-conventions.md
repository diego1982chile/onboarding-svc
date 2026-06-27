# Service Conventions

These conventions are intended for `onboarding-svc` and future Quarkus services
that follow the same Spring-style structure.

## Package Layout

Use technical layers under the service package prefix:

```text
cl.dsoto.<service>
  clients
  entities
  events/feed
  events/identity/adapters
  events/identity
  events/profile/adapters
  events/profile
  model
  repositories
  services
  services/impl
  webservice
  webservice/impl
  webservice/resources
```

Use a capability prefix when a package is specific to an upstream or business
area. For example, identity and profile events live under:

```text
cl.dsoto.onboarding.events.identity
cl.dsoto.onboarding.events.profile
```

Use `clients` for outbound REST client interfaces, client-token helpers, and
payloads that belong only to external client contracts. Raw MicroProfile REST
clients should be easy to find there instead of being buried inside a domain
event package.

```text
clients/TokenAuthRestClient.java
clients/TokenIdentityEventFeedRestClient.java
clients/ProfileEventFeedRestClient.java
clients/TokenServiceAccessTokenProvider.java
clients/resources/AccessTokenResource.java
```

Use `clients.resources` for payloads that belong only to outbound client
contracts. Prefer `*Resource` names for serialized HTTP payloads and avoid
names that make payloads look like clients.

Use capability-specific `adapters` packages when a class translates an external
transport into an application capability. For example,
`events.identity.adapters.TokenIdentityEventFeedAdapter` wraps the raw token
REST client with authorization, token renewal, one retry after `401`, and maps
the result into the identity event feed consumed by the poller.
`events.profile.adapters.ProfileEventFeedAdapter` applies the same boundary for
the profile event feed.

Use `events.feed` for generic feed polling mechanics such as cursor loading,
cursor persistence, retry orchestration, and scheduler/poller classes.

```text
events/feed/EventFeedPoller.java
events/feed/EventFeedCursorStore.java
```

Domain-specific event payloads and handlers should remain in their capability
package. For identity:

```text
events/identity/IdentityEventFeedItem.java
events/identity/IdentityEventFeedPage.java
events/identity/IdentityEventHandler.java
events/identity/adapters/TokenIdentityEventFeedAdapter.java
events/profile/ProfileEventFeedItem.java
events/profile/ProfileEventFeedPage.java
events/profile/ProfileEventHandler.java
events/profile/adapters/ProfileEventFeedAdapter.java
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

## Configuration Profiles

Use separate Quarkus property files per profile, matching the convention already
used by `token-svc`.

```text
application.properties
application-dev.properties
application-test.properties
application-docker.properties
```

`application.properties` should contain common configuration and environment
variable bindings. Avoid keeping profile-specific `%dev.*` or `%test.*` blocks
in the base file unless there is a narrow reason.

Development and test profiles may define explicit dummy values for local
operation, including service-to-service client secrets. Docker and production
profiles must read secrets from environment variables or an external secret
source, without safe-looking defaults.

For example:

```properties
# application.properties
token.service.client-secret=${TOKEN_SERVICE_CLIENT_SECRET:}

# application-dev.properties
token.service.client-secret=dev-onboarding-secret

# application-test.properties
token.service.client-secret=test-onboarding-secret

# application-docker.properties
token.service.client-secret=${TOKEN_SERVICE_CLIENT_SECRET}
```

Real secrets must never be committed. Local defaults are only for development
and tests.

## Technical Scopes

Service-to-service permissions should use capability/action names that read as
technical scopes.

```text
token.identity-events.read
```

For now, these technical scopes may be transported in the JWT authorities/groups
claim and checked with `@RolesAllowed`, together with human roles when needed.

```java
@RolesAllowed({"ADMIN", "token.identity-events.read"})
```

This is a pragmatic Quarkus/Jakarta Security convention. Conceptually,
`ADMIN` is a human role while `token.identity-events.read` is a technical
scope. If authorization grows more complex, introduce explicit scope handling
later instead of overloading endpoints with manual claim parsing.

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
events/identity
events/identity/sqs
events/profile
```

Do not put feed infrastructure directly under `events.identity` or
`events.profile`. The poll/cursor mechanics are shared infrastructure under
`events.feed`. Capability-specific feed items, mappers, handlers, and adapters
remain under their capability package.

Event envelope classes should stay close to the adapter or capability they
belong to. If a class is only needed by an SNS/SQS prototype, keep it under the
identity event integration instead of promoting it to the generic model.

Only explicit user-facing flows should emit identity events. Administrative,
support, migration, and internal maintenance actions should not emit onboarding
events by default.

## Event Feed Sources

Services that expose polling-based event feeds should follow the
[Event Feed Source Contract](event-feed-source-contract.md).

The source side of a feed owns a durable append-only event log, a cursor-based
internal HTTP endpoint, event payload versioning, technical-scope authorization,
and retention rules. The consumer side owns checkpoints and business handling.

Feed sources should use `items`, `cursor`, `nextCursor`, and `hasMore` for HTTP
feed payloads. Avoid mixing source contracts such as `events`/`sequence` with
consumer contracts that expect `items`/`cursor`.

## Tests

Mirror the production package structure in tests.

```text
src/test/java/cl/dsoto/onboarding/webservice/impl
src/test/java/cl/dsoto/onboarding/services
src/test/java/cl/dsoto/onboarding/events/feed
src/test/java/cl/dsoto/onboarding/events/identity
src/test/java/cl/dsoto/onboarding/events/profile
src/test/java/cl/dsoto/onboarding/events/identity
```

Use `@QuarkusTest` for service and webservice behavior tests, and keep assertions
focused on externally observable behavior or service contracts.
