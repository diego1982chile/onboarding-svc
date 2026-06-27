# ADR-012 - Age Verification

## Status

Accepted

## Context

The platform allows adult service providers to publish advertisements.

The business requirement is to prevent minors from publishing advertisements.
The requirement is age verification, not full identity verification.

## Decision

The MVP will implement age verification only.

Full KYC integration is explicitly out of scope for the MVP.

Age verification is not part of account onboarding. It is a publication
eligibility requirement.

Age verification is required before the first advertisement can be published.

## Scope

The system must answer this question:

```text
Is this user 18 years old or older?
```

The system does not need to answer these questions:

- Who is this person?
- Is this identity authentic?
- Is this person on sanctions lists?
- Is this person politically exposed?
- Is this person the legal owner of a specific document?

Those concerns belong to KYC solutions and are outside the scope of the MVP.

## Domain Ownership

`identity-svc` owns:

- Registration.
- Authentication.
- Authorization.
- Password management.
- Email verification.

`profile-svc` owns:

- Personal information.
- Photos.
- Age verification status.

`publishing-svc` owns:

- Advertisement creation.
- Advertisement publication.
- Validation that age verification has been completed.

## Onboarding

Onboarding is limited to:

1. Account creation.
2. Email verification.
3. Initial profile creation.

Age verification must not be implemented as an onboarding step.
Subscription selection must not be implemented as an onboarding step.

## Publication Rules

A user may:

- Create an account.
- Complete a profile.
- Upload photos.

A user may not publish advertisements until age verification has been completed
successfully.

## User Experience

The requirement must be communicated early.

Users should know from the beginning that age verification is required before
publication.

The system must never surprise users with a new verification requirement after
payment or after completing profile setup.

## Future Evolution

Future versions may introduce:

- OCR.
- Document validation.
- Selfies.
- Face matching.
- Third-party KYC providers.

The MVP must not assume any specific implementation.

The only requirement is the existence of an age verification status that can be
checked by `publishing-svc`.
