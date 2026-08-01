# Onboarding Train Contract

## Visible Steps

```text
REGISTRATION
EMAIL_VERIFICATION
PROFILE_CREATION
```

Step statuses:

```text
COMPLETED
CURRENT
PENDING
```

## Start Or Resume

The UI should start onboarding by asking for the user's email and then calling:

```http
POST /onboarding-service/api/onboarding/start
Content-Type: application/json
```

```json
{
  "email": "user@example.com"
}
```

`onboarding-svc` answers from its local onboarding projection, which is fed by
identity and profile events. It must not call `token-svc` synchronously to look
up the email.

Example response for a new email:

```json
{
  "email": "user@example.com",
  "registrationId": null,
  "state": "NEW",
  "nextAction": "COLLECT_PASSWORD",
  "train": {
    "username": null,
    "currentState": null,
    "currentStep": "REGISTRATION",
    "steps": [
      {
        "key": "REGISTRATION",
        "label": "Registro",
        "status": "CURRENT"
      },
      {
        "key": "EMAIL_VERIFICATION",
        "label": "Verifica tu correo",
        "status": "PENDING"
      },
      {
        "key": "PROFILE_CREATION",
        "label": "Crea tu perfil",
        "status": "PENDING"
      }
    ]
  }
}
```

Initial `nextAction` values:

```text
COLLECT_PASSWORD
SHOW_EMAIL_VERIFICATION_PENDING
GO_TO_LOGIN
```

`COLLECT_PASSWORD` means onboarding has no local process for the email yet.
`SHOW_EMAIL_VERIFICATION_PENDING` means a registration exists but identity has
not emitted `EMAIL_VERIFIED` yet. Once email verification is complete, the train
is the source of truth and points at `PROFILE_CREATION`. `GO_TO_LOGIN` means the
profile was created and onboarding is complete.

## Anonymous Train

```http
GET /onboarding-service/api/onboarding/train
```

```json
{
  "username": null,
  "currentState": null,
  "currentStep": "REGISTRATION",
  "steps": [
    {
      "key": "REGISTRATION",
      "label": "Registro",
      "status": "CURRENT"
    },
    {
      "key": "EMAIL_VERIFICATION",
      "label": "Verifica tu correo",
      "status": "PENDING"
    },
    {
      "key": "PROFILE_CREATION",
      "label": "Crea tu perfil",
      "status": "PENDING"
    }
  ]
}
```

## Registration Status

```http
GET /onboarding-service/api/onboarding/registrations/{registrationId}/status
```

Before the registration form is submitted, the current step is `REGISTRATION`.
After the account is created and the confirmation email is sent, the current
step is `EMAIL_VERIFICATION`. After identity confirms the registration, the
current step is `PROFILE_CREATION`.

## Authenticated Train

```http
GET /onboarding-service/api/onboarding/me/train
Authorization: Bearer <access-token>
```

The endpoint requires the `USER` or `ADMIN` role. In the current identity-only
scope, a valid access token implies that email confirmation is complete, so the
train starts at `PROFILE_CREATION`.

`PROFILE_CREATION` collects the minimum information needed to create the initial
profile: name, birth date, and location.

Plan selection, payment, active subscriptions, media uploads, profile
publication, and commercial publication-readiness checks are outside onboarding.
