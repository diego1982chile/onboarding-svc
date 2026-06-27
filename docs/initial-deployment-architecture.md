# Initial Deployment Architecture

## Context

The project is in an early stage:

- The onboarding flow is still under development.
- There are no production users.
- There are no high-availability requirements yet.
- Budget is limited.
- The domain separation should be preserved; reverting to a monolith is not the
  preferred direction.

## Domain Services

Initial backend domains:

- `identity-svc`: users, authentication, JWTs, roles, scopes, and client
  credentials.
- `onboarding-svc`: activation state, onboarding progress, and initial user-flow
  coordination.
- `profile-svc`: user profile, preferences, and editable post-onboarding data.
- `subscription-svc`: plans, subscriptions, billing, and payment-provider
  integration.

## Initial Target Deployment

Frontend:

- Oracle JET static application.
- S3 for static hosting assets.
- CloudFront for distribution.

Backend:

- Application Load Balancer as the public API entry point.
- One EC2 application node running the backend services as Docker containers.
- One separate EC2 data node for persistence.

Logical shape:

```text
Internet
  ├── CloudFront
  │     └── S3 frontend
  │
  └── ALB
        └── EC2 APP
              ├── identity-svc
              ├── onboarding-svc
              ├── profile-svc
              └── subscription-svc

EC2 APP
  └── EC2 DATA
        └── database engine
```

## Deployment Decision

Keep microservice boundaries at the application level, but simplify the initial
runtime infrastructure.

This means:

- Services remain separate deployable containers.
- Domains remain isolated.
- Service-to-service integration still happens through APIs/events.
- The first production-like deployment does not require one managed runtime or
  one managed database per service.

This avoids the fixed cost of an early Fargate/RDS setup while keeping the
domain model prepared for future scaling.

## Load Balancer

Keep the ALB in the initial architecture because it provides:

- TLS termination.
- ACM certificate integration.
- Path routing.
- Health checks.
- A single public entry point for backend APIs.

## Database Direction

The initial database should run on the EC2 data node, not inside each
application container.

The intended shape is one database engine with separate logical stores per
service, for example:

```text
database server
  identity_db
  onboarding_db
  profile_db
  subscription_db
```

or the equivalent per-engine separation mechanism.

The ownership rule still applies:

- `identity-svc` owns only `identity_db`.
- `onboarding-svc` owns only `onboarding_db`.
- `profile-svc` owns only `profile_db`.
- `subscription-svc` owns only `subscription_db`.
- No service should query another service's database directly.

### Engine Shortlist

Preferred options for a central EC2 data node:

- PostgreSQL.
- MariaDB.

Firebird was considered because it is more interesting in local or embedded
database scenarios than SQLite, especially where better write concurrency is
needed. However, if the database runs as a central server, Firebird loses much
of that operational advantage compared with PostgreSQL or MariaDB.

Current decision:

- Do not choose Firebird as the initial central database server unless a future
  spike finds a strong reason.
- Prefer PostgreSQL or MariaDB for a central self-hosted database node.

## Backups

AWS Backup should be considered early because application data has high value
even before traffic grows.

The EC2 data node should use persistent EBS volumes and snapshot/backup
automation. Backup restoration should be tested, not only configured.

## CI/CD Direction

CI:

- GitHub Actions.
- Build.
- Tests.
- Docker image build.
- Publish images to the selected registry.

CD:

- Still pending.
- GitHub Actions plus SSH is acceptable for the initial stage.
- CodePipeline/CodeDeploy are not required initially.

## Open Operational Decisions

- Container registry: ECR or another registry.
- Deployment mechanism on EC2 APP: Docker Compose, systemd-managed containers,
  or another lightweight orchestrator.
- Secret storage: SSM Parameter Store, Secrets Manager, or environment-based
  bootstrap for the initial stage.
- Logging strategy and CloudWatch retention.
- Database migration strategy.
- EC2 access strategy: direct SSH or SSM Session Manager.

## Cost Direction

The previous AWS Pricing Calculator estimate for a Fargate/RDS/ALB setup was
approximately USD 230 per month before some production-adjacent costs.

That is considered high for an application with no users.

The simplified EC2-based deployment is expected to target approximately
USD 100 to USD 150 per month, depending on:

- EC2 APP size.
- EC2 DATA size.
- EBS storage.
- Backup retention.
- Logs.
- Traffic.
- Registry/storage usage.

This estimate must be revisited after measuring real service memory usage.

## Main Risk

The main technical uncertainty is memory usage from running the initial backend
services together on a single EC2 APP node:

- `identity-svc`
- `onboarding-svc`
- `profile-svc`
- `subscription-svc`

Current sizing hypothesis:

- `t4g.medium` may be too small.
- `t4g.large` is a more conservative initial bet for multiple Java/Quarkus
  services on JVM.

This must be validated with real containers and explicit memory limits before
final sizing.

## Future Re-Evaluation

Revisit ECS/Fargate, RDS/Aurora, private subnets with NAT, VPC endpoints, and
managed deployment services only when the application has users, traffic,
availability requirements, or scaling needs that justify the extra fixed cost.
