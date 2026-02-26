# 🧱 Arquitectura

## 🧩 Visión general

ZIBE sigue un enfoque **MVVM** (Model–View–ViewModel) con separación clara entre **UI**, **ViewModels** y **repositorios**. La UI combina **Jetpack Compose** con UI tradicional (XML / Activities / Adapters) donde aplica, manteniendo una convivencia controlada durante la modernización.

---

## 🎯 Objetivos de diseño

| Objetivo | Descripción |
|---|---|
| 🧩 **Separación de responsabilidades** | UI sin lógica de datos; repositorios como frontera con Firebase/DataStore. |
| 🧪 **Testabilidad** | Lógica de validación, mapeo de errores y decisiones de dominio aislables para tests. |
| 🛡️ **Robustez y seguridad** | Manejo explícito de fallos (`ZibeResult`) y preparación para App Check. |
| 🔄 **Evolución incremental** | Refactors por partes, evitando reescrituras masivas. |

---

## 🧭 Capas y flujo de datos

```
UI (Compose / Activities / Fragments / Adapters)
        ↓  renderiza estado · despacha eventos
ViewModel
        ↓  orquesta casos de uso · expone estado y eventos
Domain (casos de uso)
        ↓  operaciones de negocio y coordinación
Data (repositorios)
        ↓  Firebase (Auth / RTDB / Storage / FCM) + DataStore
```

**Flujo típico:**
```
UI → ViewModel → (UseCase) → Repository → Firebase / DataStore
Repository → ZibeResult → ViewModel → UI (estado + mensaje)
```

**Estado y eventos:**

| Tipo | Mecanismo |
|---|---|
| Estado de pantalla | `StateFlow` |
| Eventos one-shot (snackbar, navegación, diálogos) | `SharedFlow` |

---

## 🗂️ Mapa de paquetes

> El objetivo es entender *qué vive dónde* (responsabilidad), no listar cada archivo.

### 🎨 `ui/`

Pantallas y componentes de UI por feature (auth, chat, grupos, perfil, settings, onboarding). Convive Compose con UI tradicional cuando corresponde.

### 🗄️ `data/`

Repositorios y acceso a fuentes de datos: Firebase (Auth / RTDB / Storage / FCM), DataStore (preferencias y claves locales) y coordinación de autenticación/sesión.

> Regla: si una operación puede fallar por red/IO/backend, vive acá y retorna `ZibeResult`.

### 🧠 `domain/`

Casos de uso y coordinación de negocio (perfil, sesión, salida de grupo, bootstrap de sesión, monitoreo de conflicto de sesión). Acá se decide *qué* hacer, no *cómo* se habla con Firebase.

### 🧩 `di/` y `core/di/`

Inyección de dependencias con Hilt:

- `di/` → módulos principales de binding/provisión (repositorios, Firebase, DataStore, etc.).
- `core/di/` → entry points / módulos auxiliares o transversales.

> Estado actual: conviven por evolución incremental. Dirección futura (opcional): unificar criterios en el próximo refactor de DI.

### 🧰 `core/`

Piezas transversales. Mantenerlo **particionado**, no como cajón de sastre.

| Subpaquete | Responsabilidad |
|---|---|
| `core/constants/` | Constantes, keys, nombres de nodos y convenciones compartidas. |
| `core/navigation/` | Navegación centralizada (eventos/rutas) y helpers. |
| `core/ui/` | Representación de textos/errores para UI (`UiText`) + snackbars/dispatchers. |
| `core/utils/` | Utilidades generales (`ZibeResult`, `zibeCatching`, mapeos, helpers). |
| `core/validation/` | Validadores reutilizables (email / credenciales / reglas de inputs). |
| `core/auth/` · `core/device/` · `core/chat/` | Helpers transversales que no pertenecen a una sola feature. |

### 🔔 `notifications/`

Componentes auxiliares de notificaciones (helpers, builders, canales).

> El `FirebaseMessagingService` puede estar fuera de `notifications/` por legacy/manifest. Si se mueve, hacerlo cuando se toque el Manifest para evitar regresiones.

### 🧱 `model/`

Modelos de datos (DTOs/entidades) consumidos por UI y/o persistidos en RTDB/Storage.

### 🧩 `adapters/`

UI tradicional basada en RecyclerView/Adapters (parte legacy en coexistencia controlada).

---

## 🐞 Build variants y source sets

| Source set | Rol |
|---|---|
| `src/debug/` | Código exclusivo de debug (ej.: provider/factory de App Check en modo Debug). |
| `src/main/` | Código de producción (común). |
| `src/test/` | Unit tests (JVM). |
| `src/androidTest/` | Instrumented tests (dispositivo/emulador). |

**Por qué importa:** permite que App Check tenga comportamiento distinto en debug sin contaminar release, y mantiene el repo público seguro: el CI compila con configuración dummy sin exponer credenciales.

---

## 🛠️ Manejo de errores y mensajes a UI

### `ZibeResult`

Las operaciones fallibles (red / IO / Firebase) exponen:

- `Success(data)`
- `Failure(exception)`

Los repositorios encapsulan el catching con `zibeCatching { ... }` y devuelven `ZibeResult`, evitando `try/catch` repetitivo en ViewModels.

### `UiText` (mensajes consistentes)

Abstracción para representar mensajes en UI (snackbar / diálogo):

- Strings por resource.
- Strings dinámicos.
- Mensajes mapeados desde errores (ej.: auth).

Mejora i18n, consistencia y testabilidad.

---

## 🧪 Testing

| Tipo | Scope |
|---|---|
| ✅ Unit tests (JVM) | Validadores, mappers, lógica de dominio y helpers puros. |
| ✅ Instrumented tests | Flujos que requieren Android framework / Hilt / Firestore / RTDB. |

> Recomendación: mantener fakes/mocks y módulos Hilt de test en `androidTest` cuando dependen del framework; mantener el core puro testeable en JVM.

---

## 🧩 Backend opcional: `/functions` (Cloud Functions)

El repo contempla un backend complementario en la raíz:

```
functions/
 ├─ main.py           ← entrypoint
 └─ requirements.txt  ← dependencias
```

**Rol típico:** triggers/automatizaciones desde RTDB y reglas de negocio del lado servidor cuando no conviene correrlas en cliente.

> Importante: no forma parte del módulo Android (`app/`). Es un componente complementario del sistema.

---

## 🚫 No objetivo

Este documento describe cómo se organiza el código, cómo fluye la información y qué decisiones sostienen la mantenibilidad del repo como portfolio público. No es una guía paso a paso de implementación.