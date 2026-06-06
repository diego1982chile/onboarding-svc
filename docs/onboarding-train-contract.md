# Onboarding Train Contract

## Visible Steps

```text
REGISTRATION
IDENTITY_CHECK
PLAN_SELECTION
```

Step statuses:

```text
COMPLETED
CURRENT
PENDING
```

## Public Train

```http
GET /onboarding-service/api/onboarding/public/train
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
      "key": "IDENTITY_CHECK",
      "label": "Comprueba tu identidad",
      "status": "PENDING"
    },
    {
      "key": "PLAN_SELECTION",
      "label": "Elige tu plan",
      "status": "PENDING"
    }
  ]
}
```

## Registration Status

```http
GET /onboarding-service/api/onboarding/public/{registrationId}/status
```

Before email confirmation, the current step is `REGISTRATION`. After identity
confirms the registration, the current step is `IDENTITY_CHECK`.

## Authenticated Train

```http
GET /onboarding-service/api/onboarding/me/train
Authorization: Bearer <access-token>
```

The endpoint requires the `USER` or `ADMIN` role. In the current identity-only
scope, a valid access token implies that email confirmation is complete, so the
train starts at `IDENTITY_CHECK`.
