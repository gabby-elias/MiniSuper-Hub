# Revisión y plan de refactorización arquitectónica

## Estado

El proyecto ya separa controladores, servicios, repositorios y entidades JPA. La refactorización debe mantener esa estructura y aplicar SOLID de forma pragmática, sin crear interfaces para cada clase cuando no existe una necesidad concreta de sustituir implementaciones.

En esta primera etapa:

- La lógica para validar/guardar facturas y construir detalles al agregar productos se trasladó de `FacturaController` a `FacturaService`.
- El controlador ya no consulta directamente `UsuarioRepository` para guardar facturas.
- Los cálculos y la agregación de reportes se movieron a `ReporteVentasService`.
- La generación del PDF de reportes se movió a `ReportePdfService`.
- Los reportes consultan facturas por rango de fecha y cargan sus detalles y productos en una consulta de repositorio, en lugar de cargar todas las facturas para filtrarlas en memoria.
- Se añadieron pruebas unitarias para el guardado de facturas y los cálculos del reporte.
- No se cambiaron credenciales ni se externalizaron a variables de entorno.

## Etapa 2: vistas HTML

- La lógica compartida de navegación y búsqueda de facturas se movió de `fragments/layout.html` a `static/js/layout.js`.
- La paginación/búsqueda duplicada de categorías y productos se consolidó en `static/js/catalog-list.js` mediante atributos `data-*`.
- Los scripts propios de factura, detalle de factura, formulario de producto y ticket se movieron a archivos JavaScript estáticos.
- Los estilos y la impresión del ticket se separaron en `static/css/ticket.css` y `static/js/ticket.js`.
- Se conservaron los endpoints y las reglas de autorización; no se ejecutó la aplicación ni se hicieron cambios de base de datos.

## Hallazgos y recomendaciones

### Controladores y SRP

Los controladores deberían traducir HTTP/formularios a llamadas de aplicación y preparar la respuesta. La lógica de creación de facturas estaba calculando subtotales y resolviendo clientes, productos y vendedores dentro de `FacturaController`; ya se trasladó al servicio. `ReportesController` también mezclaba cálculos, consultas y creación de PDF; ahora esas tareas están separadas.

Quedan cálculos repetidos de ventas del día y validaciones de rol en `DashboardController`, `ProductoController`, `FacturaController` y otros controladores. Como siguiente paso, conviene unificar consultas de dashboard/listados y autorización compartida.

### Servicios y reglas de negocio

`FacturaService` debe ser el punto de entrada para las operaciones que alteran facturas y sus detalles. Todavía existe CRUD independiente de `DetalleFactura`; al actualizar un detalle por ese camino, el total de la factura puede quedar desincronizado. Se recomienda decidir si los detalles solo se gestionan mediante casos de uso de factura y añadir pruebas de esa regla antes de retirar rutas o cambiar comportamiento.

La edición actual de facturas limpia la colección de detalles y la vuelve a crear. Debido a `orphanRemoval = true`, esa operación puede borrar filas de detalle existentes. No se cambió ese comportamiento en esta etapa: antes de modificarlo, hay que definir si se preservan identificadores/historial de detalles y probar la compatibilidad de la operación.

### Repositorios y rendimiento

Los repositorios CRUD simples pueden seguir extendiendo `JpaRepository`. Añadir interfaces de repositorio personalizadas solo donde una consulta específica lo justifique. El reporte ahora hace consultas limitadas por fechas, pero las métricas de categorías, clientes y productos todavía se calculan a partir de listas completas; podrían sustituirse por consultas `count` y agregaciones.

Los listados de dashboard y productos también cargan colecciones completas. Paginación y filtros son una mejora posterior, que debería preservar la interfaz actual y probar los permisos por vendedor.

### Entidades, DTO y SOLID

Varios formularios enlazan directamente entidades JPA. Migrarlos a DTOs de entrada y aplicar validación explícita reducirá acoplamiento y riesgo de asignación de campos que el usuario no debería controlar. Hacerlo por recurso y con pruebas de formulario, no como una conversión masiva.

`Producto` conserva `precio` y `precioVenta`, y sincroniza el campo legado en callbacks JPA. Además, los importes usan `double`. El cambio a un solo campo monetario y/o `BigDecimal` puede implicar transformar datos y alterar columnas; debe tratarse como migración de datos separada, no como parte de una refactorización automática.

SOLID aplicable de forma inmediata:

- **SRP:** separar entrada HTTP, casos de uso, consultas y generación de documentos; los reportes ya son un primer paso.
- **DIP:** los controladores dependen de servicios, no de repositorios concretos; se eliminó el acceso directo a `UsuarioRepository` en el flujo de guardado de facturas. No es necesario introducir una interfaz por servicio sin un motivo de sustitución o aislamiento.
- **OCP:** agregar nuevos formatos de reporte puede justificarse con componentes separados por formato si aparecen más formatos; no hace falta anticipar ese diseño hoy.
- **ISP/LSP:** no se detecta una necesidad inmediata de dividir o reemplazar las interfaces simples actuales de repositorio.

## Orden de trabajo sugerido

1. Ampliar las pruebas MVC de formularios y autorización y cubrir la consistencia entre factura y detalles.
2. Resolver la consistencia entre CRUD de detalles y total de factura.
3. Migrar formularios de entidad JPA a DTOs, con validaciones y comportamiento de error preservado.
4. Extraer consultas comunes del dashboard y los listados; añadir conteos y paginación conforme a necesidades reales.
5. Evaluar `BigDecimal`, la duplicidad de precios y la relación de roles con un plan de migración aprobado.
6. Revisar el manejo del esquema y los datos iniciales antes de cambiar la configuración de persistencia.

## Precauciones de base de datos

No ejecutar la aplicación ni tests de contexto contra una base de datos real hasta revisar estas operaciones ya existentes:

- `src/main/resources/application.properties` usa `spring.jpa.hibernate.ddl-auto=update`, que puede alterar el esquema cuando inicia Hibernate.
- `configuracion/empleadosUsersInitializer.java` migra la columna antigua `usuarios.rol` y después ejecuta `alter table usuarios drop column rol` si esa columna existe. Es una operación destructiva automática al arrancar.
- `Factura.detalles` usa `cascade = CascadeType.ALL` y `orphanRemoval = true`; reemplazar/limpiar la colección puede borrar registros de detalle.
- Eliminar facturas con su cascada puede borrar detalles relacionados.

No se cambió el esquema, no se ejecutaron migraciones y no se inició la aplicación durante esta refactorización. Las credenciales permanecen en su ubicación actual, a petición.
