# onboarding-svc

Product onboarding orchestration service built with Quarkus.

See [architecture](docs/architecture.md) and the
[onboarding train contract](docs/onboarding-train-contract.md). Shared package
and naming rules are documented in
[service conventions](docs/service-conventions.md).

The planned migration of onboarding state ownership and identity events is
documented in
[Deferred Onboarding Event Migration](docs/deferred-event-migration.md).

## Configuration

```text
CORS_ORIGINS=http://localhost:8000
JWT_PUBLIC_KEY_LOCATION=http://localhost:9091/token-service/api/.well-known/jwks.json
JWT_ISSUER=https://apis.internal.dsoto.cl
JWT_AUDIENCE=onboarding-svc
AWS_REGION=us-east-1
IDENTITY_EVENTS_TOPIC_NAME=identity-events
ONBOARDING_IDENTITY_EVENTS_QUEUE_NAME=onboarding-identity-events
ONBOARDING_IDENTITY_EVENTS_DLQ_NAME=onboarding-identity-events-dlq
IDENTITY_EVENTS_CONSUMER_ENABLED=false
IDENTITY_EVENTS_SQS_ENDPOINT_OVERRIDE=
IDENTITY_EVENTS_SQS_POLL_EVERY=1s
IDENTITY_EVENTS_SQS_MAX_MESSAGES=10
IDENTITY_EVENTS_SQS_WAIT_TIME_SECONDS=20
```

The `dev` profile uses an in-memory H2 datasource so the service can run
locally without an external database.

## Local SNS/SQS

The local event transport uses LocalStack for SNS and SQS.

Start LocalStack:

```shell script
docker compose up -d localstack
```

Provision the identity events topic, onboarding queue, subscription, and DLQ:

```shell script
sh scripts/provision-localstack.sh
```

Provisioned resources:

```text
SNS topic: identity-events
SQS queue: onboarding-identity-events
SQS DLQ: onboarding-identity-events-dlq
```

Enable the local SQS consumer against LocalStack:

```shell script
IDENTITY_EVENTS_CONSUMER_ENABLED=true \
IDENTITY_EVENTS_SQS_ENDPOINT_OVERRIDE=http://localhost:4566 \
./mvnw compile quarkus:dev
```

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/onboarding-svc-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Related Guides

- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
- SmallRye Health ([guide](https://quarkus.io/guides/smallrye-health)): Monitor service health
- Hibernate Validator ([guide](https://quarkus.io/guides/validation)): Bean validation using Hibernate Validator and Jakarta Validation annotations
- SmallRye OpenAPI ([guide](https://quarkus.io/guides/openapi-swaggerui)): Generate OpenAPI schemas and serve Swagger UI for REST API documentation

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)

### SmallRye Health

Monitor your application's health using SmallRye Health

[Related guide section...](https://quarkus.io/guides/smallrye-health)
