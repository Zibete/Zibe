# 🚀 Guía de inicio

## 🧰 Requisitos

| Herramienta | Detalle |
|---|---|
| Android Studio | Versión estable reciente. |
| JDK 17 | Requerido para compilar. |
| Android SDK | minSdk 26 · targetSdk 34. |
| Node.js 20 | Solo si vas a correr tests de reglas Firebase. |

---

## 🛠️ Setup local (Firebase real)

**1. Configurar `local.properties`**

Copiá `local.properties.example` a `local.properties` y completá:

| Variable | Descripción |
|---|---|
| `WEB_CLIENT_ID` | Client ID OAuth 2.0 (Web client) para Google Sign-In. |
| `FACEBOOK_APP_ID` | App ID de Facebook. |
| `FACEBOOK_CLIENT_TOKEN` | Client Token de Facebook. |

**2. Crear proyecto Firebase**

Registrá una app Android con `applicationId`: **com.zibete.proyecto1**

**3. Colocar `google-services.json`**

Descargá el archivo desde Firebase Console y colocalo en `app/google-services.json`.

**4. Configurar Auth**

Habilitá **Google** y **Facebook** en Firebase Auth y completá la configuración requerida por cada proveedor (OAuth / redirects) según tu proyecto.

> <!-- Captura sugerida: Firebase Console → Project settings → "Your apps" (Android) mostrando el package name -->

---

## 🧪 Modo dummy (build / CI)

Este modo existe para que el repo sea **público y seguro**.

| Estado | Detalle |
|---|---|
| ✅ Compila | Usa `app/google-services.example.json` y valores de ejemplo en `local.properties`. |
| ⚠️ No funciona en real | Firebase Auth, Realtime Database, Storage y FCM requieren credenciales reales. |

> 📌 Para ejecución completa necesitás tu `google-services.json` real y tus valores en `local.properties`.

---

## ✅ Comandos de validación

```bash
./gradlew :app:assembleDebug
./gradlew test
./gradlew lintDebug
```

**Tests de reglas Firebase (opcional):**

```bash
npm ci
npm run test:rules
```