# Ajuste de alcance: onboarding-service

## Decisión

El `onboarding-service` debe limitarse al proceso de incorporación inicial del usuario al sistema.

No debe incluir lógica de suscripción, pagos, publicación de perfil ni carga de contenido multimedia.

## Flujo actual de onboarding

1. Usuario ingresa correo.
2. Usuario ingresa contraseña.
3. Se crea usuario en estado pendiente de verificación.
4. Se envía correo de confirmación.
5. Usuario confirma correo.
6. Usuario inicia sesión.
7. Se muestra pantalla: "Cuéntanos un poco de ti".
8. Usuario ingresa la información mínima para crear su perfil:
    - Nombre
    - Fecha de nacimiento
    - Ubicación
9. Se crea el perfil básico.
10. El onboarding queda completo.

## Etapas del onboarding

```text
REGISTRATION
EMAIL_VERIFICATION
PROFILE_CREATION
```

`REGISTRATION` cubre la captura de correo y contraseña y la creación de la
cuenta pendiente de verificación.

`EMAIL_VERIFICATION` cubre el periodo en que el usuario debe confirmar su correo.

`PROFILE_CREATION` comienza después de superar `REGISTRATION` y
`EMAIL_VERIFICATION`. En esta etapa se solicita la información mínima necesaria
para crear el perfil inicial: nombre, fecha de nacimiento y ubicación.

## Responsabilidades del onboarding-service

El servicio debe encargarse de coordinar o representar el estado inicial del usuario:

- Cuenta creada.
- Correo verificado.
- Datos mínimos del perfil entregados.
- Perfil básico creado.
- Onboarding completado.

## Fuera del alcance del onboarding

No incluir en onboarding:

- Selección de plan.
- Pago.
- Suscripción activa.
- Subida de fotos.
- Subida de videos.
- Publicación del perfil.
- Validaciones comerciales sobre si el perfil está listo para ser publicado.

## Relación con otros servicios

### identity-service

Responsable de:

- Registro.
- Contraseña.
- Verificación de correo.
- Login.
- Estado `emailVerified`.

### profile-service

Responsable de:

- Crear perfil básico.
- Guardar nombre.
- Guardar fecha de nacimiento.
- Guardar ubicación.
- Gestionar contenido posterior del perfil.

### subscription-service

No forma parte del onboarding.

Debe intervenir después, cuando el usuario quiera activar capacidades comerciales, por ejemplo publicar el perfil.

## Idea clave

El onboarding responde la pregunta:

> ¿El usuario ya puede entrar y usar el sistema?

No responde la pregunta:

> ¿El perfil ya está listo para vender o publicarse?

Eso pertenece a etapas posteriores del producto.

• El comando estaba bien. El problema era el docker-compose.yml de token-svc: no estaba pasando ONBOARDING_SVC_CLIENT_SECRET al contenedor.

Ya agregué:

ONBOARDING_SVC_CLIENT_SECRET: ${ONBOARDING_SVC_CLIENT_SECRET}

Ahora vuelve a correr lo mismo:

cd /Users/diegoabelardo.soto/Repos/IdeaProjects/quarkus/token-svc
ONBOARDING_SVC_CLIENT_SECRET=test-onboarding-secret docker compose up --build

Ese error del secret vacío debería desaparecer.

─ Worked for 1m 03s ───────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────


› lo que no me gusta es...pq mierda token-svc tiene que saber de onboarding-svc ??


• Tienes razón: como diseño, no es ideal que token-svc tenga una propiedad hardcodeada para onboarding-svc.

Lo hice así por pragmatismo rápido:

token.service-client.onboarding-svc.secret=...
token.service-client.onboarding-svc.scopes=...

Funciona, pero acopla token-svc a un consumidor concreto. Mejor sería que token-svc conozca clientes técnicos, no servicios por nombre quemado en código/config específica.

Algo más sano sería:

token.service-clients.onboarding-svc.secret=...
token.service-clients.onboarding-svc.scopes=token.identity-events.read,profile.profile-events.read
token.service-clients.onboarding-svc.audiences=token-svc,profile-service

y que el código lea un mapa genérico por client_id.

O incluso mejor, si queremos mantenerlo simple ahora:

token.service-client.default.audiences=token-svc,profile-service
token.service-client.allowed-clients=onboarding-svc
token.service-client.secrets.onboarding-svc=...
token.service-client.scopes.onboarding-svc=...

La idea clave: token-svc sí debe registrar clientes autorizados, pero no debería tener código especial para onboarding como:

private static final String ONBOARDING_SERVICE_CLIENT_ID = "onboarding-svc";
@ConfigProperty(name = "token.service-client.onboarding-svc.secret")

Eso huele mal.

Yo lo refactorizaría así:

- quitar ONBOARDING_SERVICE_CLIENT_ID
- quitar config específica token.service-client.onboarding-svc.*
- introducir un ServiceClientRegistry
- resolver por client_id
- validar secret/scopes/audiences desde configuración genérica

Así token-svc sigue emitiendo tokens client-credentials, pero sin saber “onboarding” en el código.

## Retomado

Se refactorizo `token-svc` para que el endpoint `POST /api/auth/client-credentials`
resuelva clientes tecnicos desde configuracion generica:

- `token.service-client.allowed-clients`
- `token.service-client.secrets.<client_id>`
- `token.service-client.scopes.<client_id>`
- `token.service-client.audiences.<client_id>` opcional, con fallback a
  `token.service-client.audiences`

El codigo de `token-svc` ya no tiene una constante ni propiedades Java
especificas para `onboarding-svc`. `onboarding-svc` sigue existiendo solo como
un cliente configurado.

Verificado con:

```shell
./mvnw -Dtest=UserResourceTest test
```

Resultado: 18 tests, 0 failures, 0 errors.

## Rediseño conceptual de resume

El onboarding debe poder retomarse desde el correo, sin depender de
`localStorage` ni de que el usuario conserve el `registrationId`.

Se agrego el contrato:

```http
POST /onboarding-service/api/onboarding/start
```

```json
{
  "email": "user@example.com"
}
```

El endpoint responde `nextAction` para que el UI decida que pantalla mostrar:

- `COLLECT_PASSWORD`
- `SHOW_EMAIL_VERIFICATION_PENDING`
- `GO_TO_LOGIN`

La decision se toma solo con la proyeccion local de `onboarding-svc`,
alimentada por eventos/feed. No debe hacerse lookup sincronico desde
`onboarding-svc` hacia `token-svc`.
