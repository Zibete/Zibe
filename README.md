![CI](https://img.shields.io/badge/CI-private%20repo-lightgrey)
![Licencia](https://img.shields.io/badge/Licencia-MIT-lightgrey)
[![Último commit](https://img.shields.io/github/last-commit/Zibete/Zibe)](https://github.com/Zibete/Zibe/commits/main)
[![Issues](https://img.shields.io/github/issues/Zibete/Zibe)](https://github.com/Zibete/Zibe/issues)
![Kotlin](https://img.shields.io/badge/Kotlin-%E2%9C%94-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-%E2%9C%94-3DDC84?logo=android&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-%E2%9C%94-4285F4?logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%203-%E2%9C%94-757575?logo=materialdesign&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-%E2%9C%94-FFCA28?logo=firebase&logoColor=black)
![Hilt](https://img.shields.io/badge/Hilt-%E2%9C%94-34A853?logo=dagger&logoColor=white)

# 🧩 ZIBE — Mensajería Android moderna (Kotlin · Compose · MVVM · Firebase)

---

![demo](docs/assets/demo-zibe-gemini.png)

---

> ZIBE es una app Android de mensajería (**chat 1:1 y grupos**) construida como **proyecto principal de portfolio** para demostrar criterios de **arquitectura, calidad y seguridad**: UI moderna con Compose, MVVM con Flow, DI con Hilt, persistencia con DataStore e integración completa con Firebase (Auth / RTDB / Storage / FCM + App Check).

---

## 🔗 Accesos rápidos

| Recurso | Enlace |
|---|---|
| 📘 Setup & primeros pasos | [GETTING_STARTED.md](docs/GETTING_STARTED.md) |
| 🧱 Arquitectura detallada | [ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| 🔥 Esquema Firebase | [FIREBASE_SCHEMA.md](docs/FIREBASE_SCHEMA.md) |
| 🧪 Pipeline CI | [CI.md](docs/CI.md) |
| 🤝 Cómo contribuir | [CONTRIBUTING.md](CONTRIBUTING.md) |

[//]: # (---)

[//]: # (## 🎬 Demo &#40;lo visual primero&#41;)

[//]: # ()
[//]: # (> 📸 Assets sugeridos en `docs/assets/` — reemplazar los placeholders con capturas reales.)

<!-- GIF: Login → lista de chats → abrir chat → enviar mensaje -->
<!-- ![Demo Login-Chat](docs/assets/demo-login-chat.gif) -->

<!-- Imagen: ChatList con tags/estados + unread badges -->
<!-- ![ChatList](docs/assets/chatlist.png) -->

<!-- Imagen: Perfil — editar datos / foto -->
<!-- ![Perfil](docs/assets/perfil.png) -->

<!-- GIF: envío de imagen/audio en un chat -->
<!-- ![Demo Media](docs/assets/demo-media.gif) -->

---

## 🕰️ Historia del proyecto

| Etapa | Descripción |
|---|---|
| 🧱 **Origen (2020)** | App creada con UI clásica (XML / Activities / Adapters) y base Firebase. |
| 🔄 **Modernización (oct-2025 →)** | Migración progresiva a **Kotlin + AndroidX**, incorporación de **Jetpack Compose + Material 3**, refactor hacia **MVVM**, mejoras de seguridad y ordenamiento de arquitectura para dejarlo listo como repo público y mantenible. |
| 🚧 **Estado actual** | En evolución continua (refactors y mejoras por PR), con foco en compatibilidad moderna (Android 13/14) y prácticas profesionales. |

<!-- Esquema visual sugerido: "Antes (XML) → Ahora (Compose/MVVM)" con 3–5 hitos de refactor -->
<!-- ![Evolución](docs/assets/evolucion.png) -->

---

## 🧠 Qué demuestra este repo (para recruiters)

- **Arquitectura aplicada:** separación por capas (UI → ViewModel → Repositorio → Data), estado con Flow.
- **UI moderna:** Compose + Material 3 con convivencia controlada con UI clásica cuando aplica.
- **Calidad y mantenibilidad:** DI con Hilt, manejo de errores consistente, refactors por PR.
- **Seguridad para repo público:** sin credenciales reales versionadas, CI sin secretos, App Check preparado.
- **Firebase end-to-end:** Auth + RTDB + Storage + FCM con estructura documentada.

---

## ✨ Features principales

- 🔐 Autenticación con Google y Facebook (Firebase Auth).
- 💬 Chat 1:1 y grupos en tiempo real (Realtime Database).
- 🔔 Notificaciones push (FCM) con tokens por sesión.
- 👤 Perfiles, favoritos y estados de usuario.
- 🖼️ Multimedia en chats (imágenes) y soporte para audio (según implementación actual).
- ⚙️ Onboarding, búsqueda y ajustes.

---

## 🛠️ Stack técnico

### 📱 UI & Presentación
![Kotlin](https://img.shields.io/badge/Kotlin-Coroutines%20%2B%20Flow-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material%203-Design%20System-757575?logo=materialdesign&logoColor=white)

### 🧱 Arquitectura & DI
![MVVM](https://img.shields.io/badge/MVVM-StateFlow%20%2F%20SharedFlow-4CAF50)
![Hilt](https://img.shields.io/badge/Hilt-DI%20%2B%20KSP-34A853?logo=dagger&logoColor=white)
![DataStore](https://img.shields.io/badge/DataStore-Persistencia-FF6F00)

### 🔥 Backend & Servicios
![Firebase Auth](https://img.shields.io/badge/Firebase-Auth-FFCA28?logo=firebase&logoColor=black)
![RTDB](https://img.shields.io/badge/Firebase-Realtime%20DB-FFCA28?logo=firebase&logoColor=black)
![FCM](https://img.shields.io/badge/Firebase-FCM%20%2B%20App%20Check-FFCA28?logo=firebase&logoColor=black)

---

## 🚀 Cómo compilar

```bash
# Build de debug
./gradlew :app:assembleDebug

# Tests unitarios (JVM)
./gradlew test
```

> 🧪 El pipeline de CI corre automáticamente en cada push a `main`.
> <!-- Captura sugerida: GitHub Actions en verde (CI passing) -->

---

## 🔥 Firebase — Setup en 20 segundos

Este repo está listo para ser público: **no incluye `google-services.json` real**.

| Escenario | Qué hacer |
|---|---|
| ✅ Solo compilar / CI | El CI usa `app/google-services.example.json` con valores dummy. No requiere configuración adicional. |
| 🔥 Conectar Firebase real | Creá tu proyecto en Firebase Console y colocá tu `google-services.json` real en `app/`. |

> ⚠️ El modo dummy permite build y CI, pero no opera la app conectada a un backend real.

📘 Guía completa: [GETTING_STARTED.md](docs/GETTING_STARTED.md)

---

## 🗂️ Estructura del proyecto

```
📦 app/
 ├─ 📂 src/
 │  ├─ 📂 main/
 │  │  ├─ 📂 java/com/zibete/proyecto1/   ← código de producción (features + core)
 │  │  └─ 📂 res/
 │  ├─ 🐞 📂 debug/
 │  │  └─ AppCheckProviderFactoryProvider.kt   ← solo debug (App Check)
 │  ├─ 🧪 📂 test/                             ← unit tests (JVM)
 │  └─ 📱 📂 androidTest/                      ← instrumented tests
 └─ ⚙️  build.gradle(.kts)

📦 docs/    ← documentación (setup, arquitectura, firebase, CI)
📦 .github/ ← workflows + templates (Issues / PR)
```

---

## 🗺️ Próximos pasos

- 🧪 Mejorar cobertura de tests (especialmente instrumented).
- 🧭 Documentar flujos clave (onboarding, permisos, media) con capturas.
- 🎨 Unificar consistencia visual entre pantallas Compose y UI clásica.

---

## 🤝 Contribuir (feedback)

Este repositorio se publica como **portfolio / caso de estudio**.

- ✅ **Feedback, bugs e ideas**: bienvenidos vía **Issues** (con templates).
- 🔒 **Pull Requests externos**: solo por invitación, para preservar coherencia y autoría del proyecto.

➡️ Ver: **[`CONTRIBUTING.md`](CONTRIBUTING.md)**  
📌 Código de conducta: **[`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md)**

---

## ⚖️ Licencia

MIT — ver [LICENSE](LICENSE)

---

## 👤 Autor

**Matías Abel Peralta**

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Matías%20Abel%20Peralta-0077B5?logo=linkedin&logoColor=white)](https://www.linkedin.com/in/mat%C3%ADasabelperalta/)