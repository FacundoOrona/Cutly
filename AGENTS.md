# AGENTS.md — Cutly

## 1. Objetivo principal

Trabajá sobre Cutly de forma precisa, incremental y eficiente.

Prioridades:

1. Cumplir exactamente lo solicitado.
2. Preservar el comportamiento existente.
3. Evitar cambios innecesarios.
4. Minimizar archivos inspeccionados, razonamiento, comandos y consumo de tokens.
5. Entregar una solución simple y mantenible.

**No conviertas tareas puntuales en auditorías generales ni rediseños de arquitectura.**

## 2. Reglas de trabajo

- Leé este archivo al comenzar una tarea.
- Identificá el alcance concreto antes de actuar.
- Inspeccioná únicamente los archivos necesarios para resolverla.
- Seguí los patrones existentes del proyecto.
- No vuelvas a investigar decisiones o funcionalidades que ya estén implementadas y no sean relevantes.
- No repitas análisis ni informes intermedios innecesarios.
- Si el pedido es claro, implementalo directamente.
- Si falta una decisión funcional importante, preguntá antes de asumir.
- No agregues funcionalidades, validaciones o restricciones que no fueron solicitadas o aprobadas.
- No hagas refactorizaciones oportunistas.
- No modifiques código ajeno al alcance.
- No inventes endpoints, DTOs, entidades, campos ni reglas de negocio.

## 3. Contexto técnico

### Backend

- Java 17.
- Spring Boot.
- Maven.
- MySQL.
- Arquitectura organizada por dominios y capas.
- Persistencia mediante JPA/Hibernate.
- Seguridad basada en autenticación JWT y roles.

### Frontend

- React.
- JavaScript.
- Vite.
- React Router.
- Axios.
- TanStack Query.
- Tailwind CSS.

Antes de modificar cualquier funcionalidad, verificá cómo está implementada actualmente en el código. Este contexto es orientativo: el código existente es la fuente de verdad.

## 4. Reglas de arquitectura y compatibilidad

- Respetá la estructura de paquetes y componentes existente.
- Reutilizá servicios, repositorios, DTOs, validaciones y utilidades cuando corresponda.
- Mantené separadas las responsabilidades de controladores, servicios y persistencia.
- No dupliques lógica de negocio.
- No agregues dependencias sin necesidad justificada.
- No cambies configuraciones generales por conveniencia.
- No alteres contratos existentes sin que la tarea lo requiera.
- Preservá la compatibilidad con las funcionalidades existentes.
- No cambies nombres, rutas o estructuras públicas sin justificación.

## 5. Reglas de negocio

- Las reglas de negocio aprobadas por el usuario tienen prioridad.
- No deduzcas reglas nuevas a partir de decisiones parciales.
- No cambies comportamientos existentes para simplificar una implementación.
- Respetá los estados, transiciones y restricciones definidos.
- Las operaciones críticas deben conservar su atomicidad transaccional.
- Considerá concurrencia cuando una operación pueda generar duplicados o inconsistencias.
- No permitas que una operación administrativa eluda reglas de negocio de otros flujos.
- Ante contradicciones entre una nueva solicitud y una regla previamente aprobada, consultá antes de modificarla.

## 6. Backend y base de datos

- No modifiques el backend cuando la tarea sea exclusivamente de frontend.
- No modifiques endpoints existentes si no es necesario.
- No inventes contratos HTTP: verificá los controladores y DTOs reales.
- Mantené las validaciones tanto en la capa correspondiente como en el servidor.
- Usá transacciones donde la operación involucre múltiples escrituras relacionadas.
- Evitá consultas adicionales y cargas innecesarias.
- No agregues migraciones ni cambios de esquema si no son necesarios.
- Si un cambio de esquema es indispensable, explicá brevemente por qué y qué debe aplicarse.
- No asumas que se utiliza Docker: el entorno contempla MySQL instalado localmente.
- Nunca expongas, leas ni modifiques credenciales o secretos de `.env`.

## 7. Frontend

- Respetá la identidad visual actual de Cutly.
- Priorizá consistencia con los componentes y estilos existentes.
- Mantené el diseño responsive.
- Reutilizá componentes cuando tenga sentido.
- No agregues librerías visuales innecesarias.
- Gestioná correctamente estados de carga, errores y operaciones pendientes.
- Evitá duplicar llamadas HTTP o lógica de presentación.
- No cambies funcionalidades ni navegación ajenas a la pantalla solicitada.
- No sacrifiques funcionalidad por cambios visuales.

## 8. Seguridad

- Respetá siempre los roles y permisos existentes.
- No confíes en identificadores, roles o datos sensibles enviados por el cliente.
- Utilizá la identidad autenticada del usuario cuando corresponda.
- No expongas información sensible en respuestas, logs o mensajes de error.
- No debilites controles de acceso para facilitar una implementación.
- Los endpoints administrativos deben conservar la protección correspondiente.

## 9. Ejecución y consumo de recursos

**Por defecto, NO ejecutes:**

- Tests.
- Builds.
- Maven.
- Docker.
- Lint.
- Instalaciones de dependencias.
- Migraciones.
- Consultas o modificaciones de MySQL.
- Procesos que consuman recursos o tiempo innecesariamente.

Solo ejecutalos si el usuario lo solicita expresamente o si son indispensables para resolver un problema concreto. En ese caso, consultá antes de ejecutarlos.

No recorras todo el repositorio para resolver cambios localizados.

No generes documentación adicional, archivos auxiliares o reportes extensos si no fueron solicitados.

## 10. Criterio de implementación

Para cada tarea:

1. Identificá el objetivo.
2. Localizá los archivos directamente involucrados.
3. Verificá las dependencias necesarias para no romper el comportamiento existente.
4. Implementá el cambio mínimo correcto.
5. Revisá únicamente el código modificado y sus integraciones directas.
6. Finalizá con un informe breve.

No realices pasos adicionales si no aportan valor concreto a la tarea.

## 11. Informe final

La respuesta final debe ser breve y contener únicamente:

- Qué se implementó.
- Archivos modificados o creados.
- Endpoints o contratos modificados, si corresponde.
- Validaciones o decisiones relevantes.
- Problemas pendientes, si existen.
- Comandos ejecutados, si corresponde.

No incluyas:

- Resúmenes extensos de archivos inspeccionados.
- Explicaciones de código obvias.
- Repeticiones del pedido.
- Auditorías no solicitadas.
- Mejoras opcionales que no aportan al objetivo.

## 12. Regla fundamental

**Hacé exactamente lo necesario para resolver la tarea, de la manera más simple y segura posible.**

No busques demostrar cuánto analizaste. Buscá resolver correctamente el problema con el menor trabajo, tiempo y consumo de recursos posible.
