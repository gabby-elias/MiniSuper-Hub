# Diseño propuesto de API REST — MiniSuper Hub

> Documento de diseño basado en los controladores y modelos actuales. **No convierte por sí mismo los controladores MVC en endpoints REST.** Las rutas de la tabla «REST propuesta» son el contrato recomendado para una migración futura, no rutas ya implementadas. El endpoint REST público ya implementado es `GET /api/minisuper`, que devuelve la información pública del establecimiento.

## 1. Estado actual y convenciones recomendadas

La aplicación usa Spring MVC, Thymeleaf y Spring Security con sesión. La mayoría de las clases anotadas con `@Controller` devuelve nombres de plantillas y redirecciones; sus formularios envían datos y reciben `302`, no JSON. Para migrarlas:

- Mantener las páginas actuales y crear controladores REST separados con `@RestController`, preferentemente bajo `com.umoar.minisuperhub.controladores.api`.
- Usar el prefijo `/api/v1` para la API de negocio, JSON (`application/json`) y DTOs; no devolver entidades JPA directamente.
- Usar `GET`, `POST`, `PUT`/`PATCH` y `DELETE` según la operación. Responder con códigos HTTP (`200`, `201`, `204`, `400`, `401`, `403`, `404`, `409`) en vez de redirigir.
- Definir un formato uniforme de error (por ejemplo, Problem Details / RFC 9457), incluyendo validaciones y conflictos de integridad.
- Mantener las reglas de autorización y el filtrado de facturas por vendedor. No confiar en un `vendedorId` suministrado por el cliente: obtener el usuario desde `Authentication`.
- La seguridad existente es login por formulario y sesión. Si se mantiene para REST, conservar la protección CSRF en escrituras. Para clientes externos, decidir explícitamente un esquema de autenticación (por ejemplo, OAuth2/JWT); no desactivar CSRF ni abrir CORS indiscriminadamente.

La propiedad actual `springdoc.packages-to-scan` limita la especificación automática a `com.umoar.minisuperhub.controladores.api`. Por tanto, los futuros controladores REST deben ubicarse allí o se debe ajustar esa propiedad. Scalar usa el documento generado en `/v3/api-docs` y la interfaz está en `/docs`.

## 2. Autorización actual

Roles que aplica `SecurityConfig`:

| Área | Regla actual |
|---|---|
| Inicio de sesión y recursos estáticos | Público |
| Dashboard | Usuario autenticado (`ADMIN` o `EMPLEADO`) |
| Categorías | Listado para ambos; creación, edición y operaciones POST solo `ADMIN` |
| Productos | Listado para ambos; creación, edición y operaciones POST solo `ADMIN` |
| Clientes | Listado y creación para ambos; edición y eliminación solo `ADMIN`. El servicio del controlador también impide que un empleado actualice un cliente existente. |
| Facturas | `ADMIN` y `EMPLEADO`; el empleado solo debe consultar/modificar las suyas, validado por el servicio/controlador. |
| Detalles de factura | Listado y consulta de factura para ambos (con datos del vendedor filtrados); mantenimiento de líneas solo `ADMIN` |
| Reportes y usuarios | Solo `ADMIN` |
| PDF/Excel de facturas | `ADMIN` o propietario empleado, comprobado por el controlador |
| Información del minisúper (`/api/minisuper`) | Pública; solo expone nombre, dirección y teléfono del establecimiento |

En la migración, estas reglas deben aplicarse también a cada operación REST, incluidos los permisos por objeto (por ejemplo, un empleado no debe acceder a la factura de otro aunque conozca su ID).

## 3. Inventario por controlador

Las rutas bajo «Actual» corresponden a mappings observados en el código. Las rutas bajo «REST propuesta» son una API objetivo sugerida.

### `LoginController` — páginas de autenticación

- **Actual:** `GET /` y `GET /login` renderizan el formulario; si ya hay sesión, redirigen a `/dashboard`. El `POST /login` y `POST /logout` son procesados por Spring Security, no por esta clase.
- **REST propuesta:** `POST /api/v1/auth/login` (credenciales JSON y respuesta de sesión/token según la decisión de autenticación), `POST /api/v1/auth/logout` y `GET /api/v1/auth/me` (identidad y roles actuales). Mantener `GET /login` como página web.
- Nunca incluir el hash/contraseña en la respuesta de identidad.

### `DashboardController` — resumen del usuario

- **Actual:** `GET /dashboard` devuelve la vista `dashboard/index`. Calcula ventas del día en `America/El_Salvador`, cantidades de clientes/productos/facturas, productos con stock bajo (máximo cinco) y últimas facturas (máximo cinco). Las facturas dependen del rol/vendedor.
- **REST propuesta:** `GET /api/v1/dashboard/resumen` → JSON con esas métricas; filtrar facturas por vendedor para `EMPLEADO`.

### `CategoriaController` — categorías

- **Actual:** `GET /categorias`, `/categorias/nuevo`, `/categorias/editar/{id}`; `POST /categorias/guardar` y `POST /categorias/eliminar/{id}`. Las rutas de formulario devuelven vistas; guardar/eliminar redirigen a la lista.
- **REST propuesta:** `GET /api/v1/categorias`, `GET /api/v1/categorias/{id}`, `POST /api/v1/categorias`, `PUT /api/v1/categorias/{id}`, `DELETE /api/v1/categorias/{id}`.
- **DTO básico:** `id`, `nombreCategoria`. Solo `ADMIN` puede crear, editar o eliminar.

### `ClienteController` — clientes

- **Actual:** `GET /clientes`, `/clientes/nuevo`, `/clientes/editar/{id}`; `POST /clientes/guardar` y `POST /clientes/eliminar/{id}`.
- **REST propuesta:** `GET /api/v1/clientes`, `GET /api/v1/clientes/{id}`, `POST /api/v1/clientes`, `PUT /api/v1/clientes/{id}`, `DELETE /api/v1/clientes/{id}`.
- **DTO básico:** `id`, `nombre`, `documento`, `direccion`. Un empleado puede crear, pero solo `ADMIN` puede modificar o eliminar.

### `ProductoController` — productos e indicadores de lista

- **Actual:** `GET /productos` devuelve la lista y añade ventas de hoy/cantidad de productos con stock bajo para la vista. También tiene `GET /productos/nuevo`, `GET /productos/editar/{id}`, `POST /productos/guardar`, `POST /productos/eliminar/{id}`.
- **REST propuesta:** `GET /api/v1/productos` (con filtros/paginación cuando se requiera), `GET /api/v1/productos/{id}`, `POST /api/v1/productos`, `PUT /api/v1/productos/{id}`, `DELETE /api/v1/productos/{id}`.
- **DTO básico:** `id`, `nombre`, `unidadMedida`, `stock`, `precioVenta`, `categoriaId` (opcional si se admite producto sin categoría). Usar `precioVenta` como campo público; el modelo conserva además `precio` como columna heredada sincronizada, que no conviene exponer.
- Mantenimiento solo `ADMIN`. Definir validaciones de stock/precios antes de publicar el contrato.

### `FacturaController` — ciclo de vida de facturas

- **Actual:** `GET /facturas` lista y calcula indicadores; `/facturas/nuevo`, `/facturas/editar/{id}` y `/facturas/ver/{id}` devuelven formularios/detalle. `POST /facturas/guardar` valida cliente, productos y cantidades, calcula total desde los precios actuales y asigna el vendedor autenticado. También existen `POST /facturas/entregar/{id}`, `/anular/{id}` y `/eliminar/{id}`.
- **REST propuesta:**
  - `GET /api/v1/facturas` (ADMIN: todas; EMPLEADO: propias), `GET /api/v1/facturas/{id}`.
  - `POST /api/v1/facturas` para crear y `PUT /api/v1/facturas/{id}` para actualizar.
  - `PATCH /api/v1/facturas/{id}/entrega` para marcar entregada; `PATCH /api/v1/facturas/{id}/anulacion` para anular; `DELETE /api/v1/facturas/{id}` si la política permite eliminar.
- **Entrada sugerida:** `clienteId` opcional (consumidor final), `fecha` si se permite editarla y `detalles: [{ productoId, cantidad }]`. El servidor debe recalcular precio/subtotal/total; no confiar en importes enviados por el cliente. Asignar `vendedorId` desde la sesión y no permitir modificarlo desde el payload.
- **Respuesta sugerida:** factura con `id`, fecha, cliente, vendedor (sin datos sensibles), detalles, total, `anulada` y `entregada`. Determinar si anular/eliminar está permitido después de entrega.

### `FacturaPdfController` — ticket, PDF y Excel

- **Actual:** `GET /facturas/imprimir/{id}` devuelve la vista imprimible; `GET /facturas/descargar/{id}` devuelve PDF; `GET /facturas/descargar-excel/{id}` devuelve XLSX. En las tres se valida que la factura sea visible para el usuario.
- **REST propuesta:** `GET /api/v1/facturas/{id}/ticket` (HTML solo si la aplicación necesita conservar el ticket imprimible), `GET /api/v1/facturas/{id}/pdf` (`application/pdf`) y `GET /api/v1/facturas/{id}/excel` (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`). Mantener autorización por propietario.

### `DetalleFacturaController` — consultas y mantenimiento de líneas

- **Actual:** `GET /detalles-factura` lista facturas; `GET /detalles-factura/{id}` muestra una factura completa (no una línea aislada); `/detalles-factura/nuevo` y `/editar/{id}` son formularios. `POST /detalles-factura/guardar` y `/eliminar/{id}` mantienen una línea. Las operaciones de mantenimiento están restringidas a `ADMIN`; la consulta filtra por vendedor.
- **REST propuesta:** `GET /api/v1/facturas/{facturaId}/detalles`, `POST /api/v1/facturas/{facturaId}/detalles` (si se decide permitir edición individual), `GET /api/v1/detalles-factura/{id}`, `PUT /api/v1/detalles-factura/{id}`, `DELETE /api/v1/detalles-factura/{id}`.
- Evitar confundir el actual `GET /detalles-factura/{id}` —que busca una factura— con el identificador de una línea. Para facturación normal, preferir crear/actualizar detalles dentro del payload de la factura y conservar cambios de inventario/transacción en el servicio.

### `ReportesController` — resumen administrativo

- **Actual:** `GET /reportes`, solo `ADMIN`; cuenta categorías, clientes, productos, facturas vigentes/anuladas y suma ventas vigentes.
- **REST propuesta:** `GET /api/v1/reportes/resumen` con esas cantidades y total. Para periodos futuros, aceptar filtros validados como `desde` y `hasta`; no inferirlos del navegador.

### `UsuarioController` — administración de usuarios

- **Actual:** `GET /usuarios`, `/usuarios/nuevo`, `/usuarios/editar/{id}`; `POST /usuarios/guardar`, `POST /usuarios/{id}/desactivar` y `/activar`. Toda la sección es solo `ADMIN`. `UsuarioForm` contiene `id`, `username`, `nombre`, `password`, `rol` y `activo`; el servicio recibe además el administrador que realiza la operación.
- **REST propuesta:** `GET /api/v1/usuarios`, `GET /api/v1/usuarios/{id}`, `POST /api/v1/usuarios`, `PUT /api/v1/usuarios/{id}`, `PATCH /api/v1/usuarios/{id}/estado` (cuerpo `{ "activo": false }`). Solo `ADMIN`.
- Separar contraseña de los DTOs de lectura. Aceptarla solo al crear/cambiar credenciales, almacenarla siempre como hash y no devolverla nunca. Aplicar protección contra desactivar al último administrador o al usuario actual según las reglas existentes del servicio.

### `CrudAlertExceptionHandler` — errores de formularios

- **Actual:** `@ControllerAdvice` captura `DataIntegrityViolationException`, crea alertas flash y redirige a formulario/listado. No publica rutas.
- **REST propuesta:** sustituir o complementar con `@RestControllerAdvice` para los controladores API. Traducir errores de integridad a respuestas JSON (`409 Conflict` cuando corresponda), validación a `400`, no encontrado a `404` y acceso denegado a `403`; conservar el advice MVC para páginas HTML.

### `UsuarioModelAdvice` — datos comunes para Thymeleaf

- **Actual:** `@ControllerAdvice` agrega `esAdmin` y `nombreUsuario` al modelo de las vistas. No publica rutas.
- **REST propuesta:** no migrarlo como endpoint. Si se necesita la identidad actual en el cliente, exponerla de manera explícita en `GET /api/v1/auth/me`; evitar incluir datos de sesión globalmente en todas las respuestas.

### `DocumentacionController` — redirección a Scalar

- **Actual:** `GET /docs` redirige a `/docs/index.html`.
- **REST propuesta:** ninguna; mantenerlo como página de documentación, no incorporarlo al API de negocio.

### `MinisuperRestController` — información del establecimiento

- **Actual:** `GET /api/minisuper` devuelve un JSON con `nombre`, `direccion` y `telefono`, usando la configuración `app.minisuper.*`. Está anotado con OpenAPI.
- El endpoint es público porque solo expone datos generales del establecimiento. La especificación generada está en `/v3/api-docs`; Scalar la consume desde `/docs`.

## 4. Modelos y DTOs de referencia

Campos observados en entidades; son una guía, no una recomendación para serializar entidades JPA directamente:

| Recurso | Campos principales observados |
|---|---|
| Categoría | `id`, `nombreCategoria` |
| Cliente | `id`, `nombre`, `documento`, `direccion` |
| Producto | `id`, `nombre`, `unidadMedida`, `stock`, `precioVenta`, `precio` (legado), `categoria` |
| Factura | `id`, `fecha`, `total`, `cliente`, `vendedor`, `anulada`, `entregada`, `detalles` |
| Detalle factura | `id`, `cantidad`, `precioUnitario`, `subtotal`, `factura`, `producto` |
| Usuario | `id`, `username`, `nombre`, `password` (hash interno), `activo`, `roles` |
| Rol | `id`, `nombre` |

Ejemplo recomendado para crear una factura (los importes los calcula el servidor):

```json
{
  "clienteId": 12,
  "detalles": [
    { "productoId": 4, "cantidad": 2 },
    { "productoId": 7, "cantidad": 1 }
  ]
}
```

Ejemplo de respuesta de producto:

```json
{
  "id": 4,
  "nombre": "Producto de ejemplo",
  "unidadMedida": "unidad",
  "stock": 20,
  "precioVenta": 1.25,
  "categoriaId": 2
}
```

Usar `BigDecimal` en DTOs y, cuando sea posible, en la capa monetaria para precios/totales; las entidades existentes representan esos valores como `double`.

## 5. Conexión PostgreSQL actual

Configuración local observada en `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/MiniSuper-Hub
spring.datasource.username=postgres
spring.datasource.password=admin
```

La **URL JDBC completa** es:

```text
jdbc:postgresql://localhost:5433/MiniSuper-Hub
```

El usuario y la contraseña se pasan como propiedades separadas. Estos valores corresponden a la configuración local actual y **no deben reutilizarse en producción**. La contraseña está escrita en texto plano en el archivo de propiedades; migrarla a variables de entorno/secret manager y rotarla si es una credencial real.

En `compose.yaml`, la aplicación dentro del contenedor usa otra URL y credenciales obligatoriamente configuradas en el entorno:

```text
JDBC URL: jdbc:postgresql://host.docker.internal:5432/MiniSuper-Hub
Usuario:  valor de DB_USERNAME
Contraseña: valor de DB_PASSWORD
```

## 6. Lista de trabajo sugerida para la migración

1. Crear DTOs de entrada/salida y validaciones; no exponer entidades ni hashes.
2. Implementar primero categorías, clientes y productos con `ResponseEntity` y pruebas MockMvc.
3. Definir transacciones para factura + detalles + inventario y calcular los importes en servidor.
4. Aplicar pruebas de autorización por rol y propiedad de factura para cada endpoint.
5. Añadir `@RestControllerAdvice` y respuestas de error consistentes.
6. Documentar los controladores REST con anotaciones OpenAPI y verificar el esquema en `/v3/api-docs` y Scalar en `/docs`.
7. Decidir autenticación, CSRF, CORS, paginación y versionado antes de exponer la API fuera del mismo origen.
