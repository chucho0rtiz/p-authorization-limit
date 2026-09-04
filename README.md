Dependencias a seleccionar:
- Spring Reactive Web (spring-boot-starter-webflux) — obligatoria
- Spring Data R2DBC — acceso reactivo a Postgres
- PostgreSQL Driver (r2dbc-postgresql)
- Validation (spring-boot-starter-validation) — para validar los DTOs de entrada
- Actuator — te resuelve gran parte del punto 8 (diagnóstico/estado) sin esfuerzo

Estructura de paquetes: 
capas claras y pequeñas, coherente
- exception:  manejo de errores centralizado.
- dto: request/response separados de las entidades de dominio.
- domain (o model): entidades/registros (Customer, Authorization).
- repository: interfaces.
- service: la lógica de negocio.
- api (o web/controller): los contraladores para los 4 endpoints.
- config: seguridad del endpoint admin.

EndPoints:
- GET /customers/{customerId}/limit
- POST /authorizations
- GET /authorizations/{transactionId}
- PATCH /customers/{customerId}/limit

Codigos de respuesta:
- 500 INTERNAL_ERROR
- 404 CUSTOMER_NOT_FOUND - AUTHORIZATION_NOT_FOUND
- 403 FORBIDDEN_ADMIN_OPERATION
- 409 CUSTOMER_INACTIVE - INSUFFICIENT_FUNDS - TRANSACTION_CONFLICT
- 400 INVALID_AMOUNT - LIMIT_BELOW_CONSUMED
- 200 SUCCESS

problema identificado:
- Se deberia de manejar un estado por cliente Activo/Inactivo, esto con el fin de identificar si puede hacer acciones. En esta prueba no hice el proceso de cambio de estado pero queria tenerlo en el radar.
- Se creara un campo que maneje el rol de los posibles usuarios que puedan acceder a la opcion de cambio de limite diaria.
- Para esta practica se decidio no mantener las transacciones que fallen registradas en base de datos, solo se usan los logs para manter trazabilidad. En caso de que se requiera yo crearia otra tabla para manter los errores de forma mas detallados o usar la misma tabla con un estado y uno o dos campos que describan el error pero que puedan ser NULL para no afectar la informacion de los movimientos aceptados. 
- Para el caso en el que el transactionId pueda llegar repetido con informacion diferente, yo arrojaria un error para mencionar que la transaccion ya existe y el clinete pueda verificar su cuenta, esto iria relacionado con el flujo de indepotencia en el que si el transactionId ya existe y viene con el mismo valor solo mostraria el valor que existe en la tabla o otra posibilidad seria que arrojaramos el mismo error anteriormente mencionado u uno diferente.

alternativa seleccionada:
- R

alternativas consideradas cuando corresponda:
- R

motivo de la decisión:
- R

trade-offs:
- R

riesgos:
- R

cómo verificó que funciona:
- R

