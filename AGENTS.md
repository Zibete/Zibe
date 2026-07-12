# AGENTS.md — Reglas del repo ZIBE para agentes de código

## Regla de oro

- No inventar UX ni arquitectura. Si algo es ambiguo, preguntar antes de actuar.
- Preferir diffs mínimos que respeten los patrones ya establecidos.
- Nunca eliminar código existente sin pedido explícito: Si hay que eliminar algo obsoleto, informar.
- Antes de implementar algo nuevo (módulos DI, helpers, repositorios, componentes UI, tests), **buscar si ya existe una implementación equivalente** en el repo.
  - Revisar módulos Hilt existentes para evitar bindings duplicados.
  - Preferir reutilizar/expandir lo existente antes que crear “otra versión”.
  - Si hay solapamiento, proponer consolidación en vez de duplicación.
---

## Lenguaje y estilo

- Preferir Kotlin; no crear Java nuevo.
- Nombres en inglés, funciones pequeñas, sin código muerto.
- Omitir `{}` cuando sea posible (lambdas de una línea, funciones de expresión).
- Preferir `=` en vez de `return` + bloque cuando aplique.
- Agregar entradas de configuración para nuevos destinos en lugar de depender de defaults.

---

## Higiene al tocar archivos (obligatorio)

Cuando se modifique cualquier archivo (Kotlin o XML), el cambio debe dejar el archivo “limpio”:

- Eliminar imports sin uso (Optimize Imports: Ctrl + Alt + O)
- Reformat del archivo (Reformat Code: Ctrl + Alt + L) para que quede con indentación/espaciado consistente.
- Prohibido commitear imports en gris o warnings triviales evitables si el IDE puede resolverlos automáticamente.
- No reformat masivo del repo: solo aplicar estas acciones sobre archivos que ya fueron tocados por el objetivo del PR.

---

## Manejo de errores — ZibeResult


Siempre que una operación pueda fallar, usar `ZibeResult`. Es la fuente de verdad para errores en el repo/UseCases.

- **El repositorio es el único responsable del catching.** Todas las consultas a Firebase quedan envueltas en `zibeCatching` dentro del repo. Ninguna capa superior hace `try/catch` manual.
- **El repositorio devuelve siempre `ZibeResult<T>`.** No devuelve valores planos cuando existe posibilidad real de error (red, permisos, etc.).
- Usar `zibeCatching { }` para envolver lógica que puede lanzar excepción.
- Encadenar con `.onSuccess { }`, `.onFailure { }`, `.onFinally { }`.
- No usar `try/catch` manual si `zibeCatching` alcanza.
- Los repositorios **siempre** deben retornar `ZibeResult<T>` cuando la operación es fallible.
- **No se usan fallbacks silenciosos** (`getOrDefault`) cuando el error debe mostrarse. Si el error es relevante para la UX, se propaga como `Failure`. Si en algún caso se decide fallback, debe hacerse conscientemente en el repo y sin perder coherencia.
- **Se evita mezclar estrategias.** No se combinan `getOrDefault` y `onFailure` en el mismo flujo si eso implica perder el error real.
- **Estados complejos (ej: `BlockState`) no deben tener defaults ambiguos.** En lugar de devolver un estado "todo false" por defecto, se prioriza:
    - Propagar `Failure`, o
    - Definir estados explícitos como `Unknown`.

---

## ViewModels

- Estado de UI: `StateFlow` (expuesto como `StateFlow`, backing con `MutableStateFlow`).
- Eventos one-shot: `SharedFlow` (sin replay) o `Channel` según patrón del módulo; no mezclar en el mismo VM.
- Corrutinas: `viewModelScope.launch` como base.
- Cuando se llama a un repo o función fallible: `viewModelScope.launch { zibeCatching { } .onSuccess { } .onFailure { } }`.
- **Manejo de Errores en ViewModel:**
    - El ViewModel maneja la presentación del error.
    - Usar `onFailure` para capturar el `Throwable` y emitir snackbar.
    - Usar `onSuccess` para actualizar el `uiState`.
- No usar `LiveData`. No usar `GlobalScope`.

---

## Repositorios

- Siempre separar interfaz e implementación (`FooRepository` + `FooRepositoryImpl`).
- Las funciones fallibles retornan `ZibeResult<T>`.
- Las funciones reactivas retornan `Flow<T>` (sin `ZibeResult` en el tipo, manejar errores en el `catch` del flow).
- Inyectar siempre vía constructor (Hilt se encarga del binding).

---

## Inyección de dependencias

- Solo Hilt. Sin Koin, sin DI manual.
- Usar `@HiltViewModel` en ViewModels.
- Módulos en el paquete `di/`.

---

## Navegación

- Navigation: NavComponent (Fragments) o Navigation Compose; pasar IDs y resolver en VM con SavedStateHandle.
- No pasar objetos complejos por argumentos: pasar IDs y resolverlos en el ViewModel vía `SavedStateHandle`.
- No usar `newInstance()` en Fragments. Usar `arguments` + `SavedStateHandle`.

---

## Separación de capas

- Criterio pragmático: separar cuando aporte claridad real, no por formalismo.
- Como mínimo: `presentation` (ViewModel + Fragment/Activity) y `data` (repositorios + fuentes).
- La capa `domain` se agrega solo si la lógica de negocio justifica casos de uso explícitos.


## Entregables esperados del agente

Al finalizar cualquier tarea, el agente debe listar:

- Archivos modificados (con ruta relativa).
- Archivos nuevos creados (si aplica), con justificación.
- Checklist de prueba manual.
- Si se pide PR: Crear el archivo md en la raíz del proyecto según las instrucciones

## 🤖 Instrucciones para Creación de Pull Requests (PR)

Siempre que se solicite la creación de un PR, el agente debe generar un archivo `.md` en la raíz del proyecto (sugerido: `PR_[nombre_del_repo]_[timestamp].md`) y redactarlo con el estilo real del repo:

- **Idioma:** cuerpo, headings y explicación en **español (es-AR)**. Dejar en inglés solo `type(scope)`, nombres de módulos/paths/clases/comandos y subjects de commit citados literal.
- **Tono:** técnico, directo y breve. Priorizar bullets concretos; evitar prosa larga, marketing o contexto redundante.
- **Encabezado:** abrir con un título estilo PR real del repo: `# [emoji] type(scope): resumen` o `## [emoji] PR: ...`. Debajo, incluir `Base:` y `Rama:` en líneas separadas.
- **Secciones base:** incluir siempre `[emoji] Qué se cambió` y `[emoji] Commits incluidos`.
- **Secciones habituales:** agregar `[emoji] Por qué`, `[emoji] Validación manual` y `[emoji] Fuera de alcance` cuando aporten valor real. No forzar secciones vacías en PRs chicos o de docs.
- **Secciones opcionales:** usar `[emoji] Validación Local` para comandos realmente ejecutados, `[emoji] Cómo correr los tests` si conviene dejar receta reproducible, `Tests agregados` para aclarar si hubo tests nuevos o no, `Cambios de API/Contratos` cuando cambien interfaces/wiring/source of truth, y `Código muerto/paths duplicados` cuando se haya eliminado o consolidado algo.
- **Nivel de detalle:** en `[emoji] Qué se cambió`, agrupar por módulo/feature/área y nombrar archivos, clases, contratos o workflows tocados. En `Por qué`, listar el problema resuelto o la razón técnica/operativa. En `Fuera de alcance`, dejar follow-ups reales o límites decididos para mantener foco.
- **Bullets y checkboxes:** usar bullets cortos por defecto. Reservar checkboxes para validación manual o checklists; fuera de eso, usarlos solo si ordenan mejor el cambio.
- **Validación:** `Validación manual` debe ser una checklist de flujos concretos y observables. Si hubo validación local automatizable, agregar comandos exactos en bloque `bash`. No duplicar `Validación Local` y `Cómo correr los tests` si muestran lo mismo.
- **Commits incluidos:** listar `hash corto + subject`. Si el PR es de un solo commit, una sola línea alcanza.
- **Títulos, ramas y commits:** preferir ramas en minúsculas con prefijo de tipo (`fix/...`, `refactor/...`, `docs/...`, `chore/...`, `feat/...`, `build/...`, `ci/...`, `test/...`, `codex/...` cuando corresponda). Alinear, cuando aplique, título del PR y commits con formato `type(scope): summary`.
- **Emojis:** usarlos de forma pragmática para mejorar escaneo. No imponer un set fijo de gitmojis ni una correspondencia rígida por sección.
- **Evitar:** secciones vacías o “Ninguno” repetido sin valor, headings en inglés por estilo, cuerpos largos sin módulos/archivos concretos, y secciones inventadas que no aparezcan de forma recurrente en el repo.

---

### Referencia para Code Reviews
Cuando actúes como revisor, usa estos iconos en tus comentarios:
- 💡 `:bulb:` Sugerencia opcional.
- ⚠️ `:warning:` Problema no crítico.
- 🚫 `:no_entry_sign:` Cambio obligatorio (Bloqueante).
- ❓ `:question:` Duda o aclaración.
- 🙌 `:raised_hands:` Felicitación.
- 🧹 `:broom:` Limpieza menor.

---

## Herramientas disponibles (Windows)

Las siguientes CLI están instaladas y disponibles en PATH:

- `rg` (ripgrep) — búsqueda en código
- `fd` — búsqueda de archivos
- `fzf` — selección interactiva
- `bat` — inspección de archivos
- `jq` — procesamiento de JSON
- `gh` (GitHub CLI) — operaciones git y PRs

Se recomienda usarlas para búsqueda, inspección y operaciones sobre el repo antes de editar.

## Tests (Unit / Instrumentation)

- **No crear tests** (unit tests / instrumentation tests) **a menos que el usuario lo pida explícitamente**.
- Si el cambio toca lógica crítica y *sería ideal* agregar tests, el agente debe:
  1) **No implementarlos**
  2) **Dejarlo listado** como "Deuda técnica"
  3) Proponer **qué test** agregaría y **dónde**, en bullets (sin código).
- Excepción: solo crear tests si el usuario lo pidió, o si la tarea dice explícitamente "agregar tests" / "dejar listo para test".
- Tests: Preferir @TestInstallIn + @UninstallModules para evitar repetir @BindValue en múltiples clases

---

## Estado arquitectónico permanente de ZIBE

### Módulos y dirección

- `app` depende de `data`, `domain`, `core:common` y `core:designsystem`.
- `data` depende de `domain` y `core:common`; es dueño de Firebase, DataStore,
  snapshots, paths y referencias.
- `domain` solo depende de `core:common`; no importa Android, Firebase,
  proveedores sociales, Hilt ni implementaciones.
- `core:common` contiene resultados/utilidades puras y `core:designsystem` el
  sistema visual compartido.
- Ante una capacidad nueva, extender la abstracción dueña. No saltar capas ni
  crear un helper/repositorio paralelo para evitar modificar el contrato.
- Ejecutar `python scripts/check_architecture.py` al tocar límites o DI.

### Estado, eventos y errores

- Una pantalla compleja expone un `StateFlow` principal e inmutable.
- One-shots usan `SharedFlow` sin replay o `Channel`; no mezclar ambos en el
  mismo ViewModel.
- El repositorio captura con `zibeCatching` y devuelve `ZibeResult`; el
  ViewModel decide presentación. `CancellationException` siempre se relanza.
- `Context`, `Activity`, `Uri`, launchers, permisos y media quedan en el borde
  Android. Los ViewModels no los retienen.
- Usar el snackbar host global; no agregar hosts o collectors paralelos.

### Contrato Firebase/DM

- Paths y ownership: `docs/FIREBASE_SCHEMA.md`.
- `ChatRefs` y tipos Firebase no salen de `data`.
- DM nace `MSG_DELIVERED (1)`; solo Android receptor confirma
  `MSG_RECEIVED (2)`; `MSG_SEEN (3)` exige lectura real o `activeThread` fresco.
- `messaging.send()` exitoso no equivale a receipt.
- Mensaje y dos resúmenes DM se escriben en un fan-out raíz atómico; unread
  usa incremento de servidor; estados nunca retroceden.
- Conservar Groups/group chat legacy salvo pedido explícito.
- Todo cambio de contrato debe alinear Android, Functions, Rules, tests y docs.

### Paths relevantes

- Presentación/wiring: `app/src/main/java/com/zibete/proyecto1/`.
- Contratos/casos de uso: `domain/src/main/java/`.
- Implementaciones/backend local: `data/src/main/java/`.
- Functions: `functions/main.py`; Rules: `database.rules.json`.
- Tests: `*/src/test`, `app/src/androidTest`, `functions/tests` y
  `tools/firebase-rules-tests`.
- Decisiones: `docs/ARCHITECTURE.md`, `docs/adr/` y
  `docs/audits/PRE_SCALE_COHERENCE_REVIEW.md`.

### Matriz por tipo de cambio

- Siempre: `python scripts/check_architecture.py` y `git diff --check`.
- Functions: `python -m py_compile functions/main.py` y
  `python -m unittest discover functions/tests`.
- Rules/contrato RTDB: `npm run test:rules`.
- Kotlin/lógica: `gradlew.bat testDebugUnitTest --no-daemon`.
- UI/recursos/manifest: `lintDebug`, `:app:assembleDebug` y
  `:app:compileReleaseKotlin`.
- DI/androidTest: `:app:compileDebugAndroidTestKotlin`.
- Flujos Android, si hay dispositivo: `:app:connectedDebugAndroidTest`.

La validación física debe cubrir lo afectado entre credenciales reales, dos
usuarios DM, push background/killed, camera/crop, micrófono, permisos y retorno
desde Settings. No afirmar acciones humanas no realizadas; dejar el PR draft si
queda una validación obligatoria pendiente.

### Restricciones para agentes

- No dejar placeholders, stubs, callbacks vacíos, `TODO` nuevo sin issue, logs
  temporales ni código comentado como implementación futura.
- No deployar Functions/Rules/Hosting, mergear, crear release o tag sin
  autorización explícita.
- Para trabajo Android no trivial, usar `$zibe-android-engineering` como guía
  reutilizable, subordinada a este archivo y al pedido concreto.

### Reporte de iteración

Informar archivos modificados/nuevos, decisiones y contratos, comandos con
resultado exacto, validación manual completada/pendiente, riesgos, deuda y
confirmación de que no hubo deploy. Si existe PR, incluir rama, commits, URL y
estado draft/ready.
