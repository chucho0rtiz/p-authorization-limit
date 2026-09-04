# Authorization Service

Servicio backend para controlar autorizaciones de consumo de clientes contra un límite diario, construido con Java + Spring Boot + Spring WebFlux + Spring Data R2DBC sobre PostgreSQL (Supabase).

## Tecnologías

- Java 21
- Spring Boot (Spring WebFlux, Spring Data R2DBC, Validation, Actuator)
- PostgreSQL (hospedado en Supabase), accedido de forma 100% reactiva vía R2DBC
- Maven

## Cómo ejecutar

1. Tener un proyecto de Supabase (o cualquier PostgreSQL accesible) con el esquema descrito en `schema.sql` / las tablas `customers` y `authorizations`.
2. Definir las siguientes variables de entorno (en la Run Configuration de tu IDE o en el entorno de ejecución):
   - `DB_HOST`
   - `DB_PORT`
   - `DB_NAME`
   - `DB_USER`
   - `DB_PASSWORD`
3. Ejecutar `PJavaWebfluxApplication` (o `mvn spring-boot:run`). El servicio levanta en `http://localhost:8080`.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| `GET` | `/customers/{customerId}/limit` | Consulta el límite diario, consumo y disponible de un cliente (RF01) |
| `POST` | `/authorizations` | Solicita la autorización de un consumo (RF02, RF03, RF04) |
| `GET` | `/authorizations/{transactionId}` | Consulta una autorización previamente aprobada (RF05) |
| `PATCH` | `/customers/{customerId}/limit` | Modifica el límite diario de un cliente; requiere header `X-Admin-Customer-Id` de un cliente con `role_admin=true` (RF06) |

## Códigos de respuesta

| HTTP | Código | Cuándo ocurre |
|---|---|---|
| 200 | — | Operación exitosa |
| 400 | `INVALID_AMOUNT` | Campo requerido faltante/vacío, o `amount` ≤ 0 |
| 400 | `LIMIT_BELOW_CONSUMED` | El nuevo `dailyLimit` (RF06) es menor al `consumedAmount` actual del cliente |
| 403 | `FORBIDDEN_ADMIN_OPERATION` | Quien llama a RF06 no existe como cliente o no tiene `role_admin=true` |
| 404 | `CUSTOMER_NOT_FOUND` | El `customerId` no existe |
| 404 | `AUTHORIZATION_NOT_FOUND` | El `transactionId` no existe (nunca se procesó, o fue rechazado) |
| 409 | `CUSTOMER_INACTIVE` | El cliente existe pero `state=false` |
| 409 | `INSUFFICIENT_FUNDS` | El cliente no tiene saldo disponible suficiente |
| 409 | `TRANSACTION_CONFLICT` | El `transactionId` ya existe con un `customerId`/`amount` distinto al solicitado |
| 500 | `INTERNAL_ERROR` | Error no controlado; se registra el detalle en logs, nunca se expone al cliente |

## Estructura de paquetes

- `domain`: entidades de persistencia (`Customer`, `Authorization`).
- `dto`: objetos de entrada/salida de la API, desacoplados de las entidades de persistencia.
- `repository`: interfaces `ReactiveCrudRepository`, incluyendo los `@Query` custom para las actualizaciones atómicas.
- `service`: lógica de negocio (`AuthorizationService`, `CustomerService`).
- `api`: `@RestController` de los 4 endpoints.
- `exception`: excepciones de dominio + `GlobalExceptionHandler` centralizado.

## Decisiones técnicas

### 1. Persistencia: PostgreSQL administrado en Supabase, vía R2DBC

**Problema identificado**: La prueba no exige una base de datos específica, pero el servicio necesita persistencia real y consistente entre reinicios, y compatible con el modelo no bloqueante de WebFlux.

**Alternativa seleccionada**: PostgreSQL gestionado en Supabase, accedido con Spring Data R2DBC (driver `r2dbc-postgresql`).

**Alternativas consideradas**:
- Almacenamiento en memoria: se descartó porque no sobrevive un reinicio y no permite demostrar de forma realista las pruebas requeridas.
- PostgreSQL local: se descartó por el tiempo de configuración adicional (instalar/levantar el contenedor) frente al costo cero de usar un Postgres ya administrado en Supabase.

**Motivo de la decisión**: Supabase da acceso a un Postgres real en minutos, sin infraestructura propia, y permite apoyar la estrategia de concurrencia directamente en las garantías transaccionales de Postgres.

**Trade-offs**: se depende de un proveedor externo, con la latencia de red que eso implica frente a una base de datos local o en memoria. Esto con el fin de hacer más realista el ejercicio, ya que en un ambiente productivo se estará dependiendo de configuración internas de los servidores en los que despleguemos la DB.

**Riesgos**: la capa gratuita de Supabase no nos asegura una estabilidad en todo momento, para resolver esto tendríamos que pagar una suscripción o migrar a AWS, un contenedor Docker u otras alternativas mejores.

**Cómo se verificó**: conexión probada primero desde el Database Tool de IntelliJ, y luego con los 4 endpoints reales contra datos insertados directamente en Supabase, confirmando que los cambios de saldo persisten entre peticiones.

### 2. Concurrencia: UPDATE SQL atómico condicional, sin locks en memoria

**Problema identificado**: dos solicitudes concurrentes sobre el mismo cliente no pueden en conjunto autorizar más del saldo disponible; y clientes distintos no deben bloquearse entre sí.

**Alternativa seleccionada**: una única sentencia UPDATE customers, verificando cuántas filas fueron afectadas (`CustomerRepository.tryConsume`).

**Alternativas consideradas**:
- Lock en memoria por cliente: descartado porque no escala si el servicio corre en más de una instancia, y encaja mal con el modelo reactivo (bloquear un hilo contradice el propósito de WebFlux).
- MySQL: es una buena opción, pero al revisarlas más a profundidad, determiné que me demoraría más en configurar y que Postgres era ventajoso por el uso de R2DBC.

**Motivo de la decisión**: Postgres nos permite tener más ventaja por el uso de R2DBC que va especialmente con atomicidad que requerimos en el reto.

**Trade-offs**: la fila de un cliente se convierte en un punto de serialización — bajo un volumen extremadamente alto de solicitudes concurrentes *para el mismo cliente*, estas se procesan en fila (una tras otra), nunca en paralelo real.

**Riesgos**: Esta solución es segura solo si todas las partes del sistema modifican el saldo usando exactamente la misma consulta con el WHERE.

**Cómo se verificó**: se replicó el ejemplo exacto del PDF (cliente con $100.000 disponibles, dos solicitudes de $70.000 y $60.000) disparadas **verdaderamente en paralelo** con PowerShell (`Start-Job`). En ambos casos, una solicitud se aprobó y la otra se rechazó por `INSUFFICIENT_FUNDS`, y el saldo final nunca quedó negativo ni se descontó de más.

### 3. Idempotencia: solo se persisten autorizaciones aprobadas

**Problema identificado**: una retransmisión de la misma solicitud no debe descontar el saldo dos veces; y el PDF no especifica qué hacer con los intentos rechazados.

**Alternativa seleccionada**: la tabla `authorizations` solo recibe una fila cuando la operación fue **aprobada**. La detección de repetidos usa `transaction_id` (columna `UNIQUE`): si ya existe con los mismos datos, se devuelve el mismo resultado sin volver a descontar; si existe con datos distintos, se responde `409 TRANSACTION_CONFLICT`. Como defensa adicional ante una carrera muy ajustada entre dos solicitudes idénticas simultáneas, el `INSERT` captura la violación del `UNIQUE` (`DuplicateKeyException`) y responde igual que si hubiera sido idempotente desde el principio.

**Alternativas consideradas**: persistir todo intento (aprobado y rechazado) agregando columnas `status`/`errorMessage` a `authorizations` — más completo para la prueba, pero nos salimos un poco de lo planteado.

**Motivo de la decisión**: una autorización rechazada no tiene ningún efecto (no se descontó nada), por lo que no existe riesgo de "doble descuento" que proteger con idempotencia; los rechazos ya quedan trazados en los logs de `GlobalExceptionHandler`.

**Trade-offs**: `GET /authorizations/{transactionId}` de una transacción que fue rechazada devuelve `404 AUTHORIZATION_NOT_FOUND` — el mismo código que si esa transacción nunca hubiera sido enviada.

**Riesgos**: si un requisito futuro exige persistencia completa de todos los intentos (aprobados y rechazados), este diseño requeriría un cambio de esquema y de flujo.

**Cómo se verificó**: prueba manual confirmando que tras un rechazo (ej. fondos insuficientes) no queda fila en `authorizations`, y que consultar ese `transactionId` después devuelve `AUTHORIZATION_NOT_FOUND`; y prueba de repetición exacta de una autorización aprobada, confirmando que el saldo del cliente no cambia en el segundo intento.

### 4. Autorización administrativa: `role_admin` en `customers` + header `X-Admin-Customer-Id`

**Problema identificado**: la operación de modificar el límite diario debe ser administrativa y no estar disponible para un consumidor ordinario.

**Alternativa seleccionada**: se agregó la columna `role_admin` a la tabla `customers`. Quien llama a `PATCH /customers/{customerId}/limit` se identifica mediante el header `X-Admin-Customer-Id`; el service busca ese `customerId` y exige `role_admin=true` antes de aplicar el cambio.

**Alternativas consideradas**: manejar la autorización por medio de un token JWT con un módulo separado, creando un inicio de sesión obligatorio para cada usuario, separando la lógica de un usuario y cliente.

**Motivo de la decisión**: se decidió ser más proporcional a las limitaciones de la prueba y no salirse tanto del contexto brindado.

**Trade-offs**: mezcla dos responsabilidades distintas en una sola entidad — el "cliente" cuyo consumo se controla y la "identidad" que administra el sistema. En un diseño más limpio, estarían separadas.

**Riesgos**: cualquier fila de `customers` con `role_admin=true` puede ejecutar la operación administrativa con solo conocer su `customerId`.

**Cómo se verificó**: prueba con un `customerId` con `role_admin=true` (200 OK, límite actualizado) y con uno sin ese rol.

## Supuestos y ambigüedades

### 1. Estado de los clientes:

**Qué información falta**: no se menciona si los usuarios siempre estarán activos o en algún momento los dejarán fuera del sistema.
**Qué preguntaría al responsable funcional**: ¿los usuarios deben de ser eliminados junto con sus registros o se debe persistir la información por trazabilidad?
**Qué supuesto utilizó temporalmente para poder continuar**: supuse que no es necesario eliminar la persistencia por un posible uso de generación de reportes o simplemente para trazabilidad interna o auditorías.

### 2. Existencia de un único usuario ADMIN:

**Qué información falta**: no se menciona en ningún momento que solo debe de haber una persona con rol administrador.
**Qué preguntaría al responsable funcional**: ¿cualquiera puede ser administrador o quiénes son los que ocupan este rol dentro del aplicativo?
**Qué supuesto utilizó temporalmente para poder continuar**: trabajé sobre el supuesto que líderes o altos mandos podrían ser administradores y cambiar límites diarios.

### 3. EndPoint de actualización de límite y respuesta:

**Qué información falta**: no se menciona en ningún momento cómo debe de ser llamado el endpoint de actualización de límite ni qué respuesta debía obtener.
**Qué preguntaría al responsable funcional**: ¿cuándo un usuario con rol admin actualice el límite de un usuario qué debe responder el sistema?
**Qué supuesto utilizó temporalmente para poder continuar**: la respuesta la creé usando el mismo modelo de consultar el límite para mantener consistencia en las respuestas.

## Uso de IA
- Usé Claude para verificar qué lineamientos de código me hacía falta cumplir dentro de la prueba técnica, dándole acceso a mi repositorio y al PDF de la prueba técnica, permitiéndole darme consejos y posibles mejoras sobre lo desarrollado.
- Usé OpenCode con la integración de IA free para verificar la realización de documentación del README y mejorar la redacción y escritura del documento para que sea más legible.
- Le di a la IA el schema de mi DB para que me generara información de muestra para poder usar en la prueba y no tener que crear todo manualmente.
- Generé con Claude los scripts que ejecutan la prueba de concurrencia solicitada en la prueba técnica para que solo tenga que ejecutarla con un doble clic con un ejecutable .bat.

## Pruebas automatizadas

*(pendiente — ver sección 9 del enunciado)*
