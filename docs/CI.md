# 🤖 CI (Integración continua)

Este repositorio usa GitHub Actions para validar que **compila**, que los **tests pasan** y que no se introducen regresiones de **lint**, sin requerir credenciales reales.

---

## 🧩 Workflow

| Campo | Valor |
|---|---|
| 📄 Archivo | [`/.github/workflows/android-ci.yml`](../.github/workflows/android-ci.yml) |
| 🏗️ Job principal | `build-test-lint` |

---

## ✅ Qué valida (pipeline)

**1. Checkout + validación del Gradle Wrapper**
Previene modificaciones maliciosas del wrapper.

**2. Configuración dummy para CI (sin secretos)**
- Crea `local.properties` con valores de ejemplo.
- Crea `app/google-services.json` copiando desde `app/google-services.example.json`.

> Permite compilar en CI sin conectar el repo público a un Firebase real.

**3. Toolchain**
JDK 17 (Temurin) + cache de Gradle · Node.js 20 (para tests de reglas Firebase).

**4. Tests de reglas Firebase**
```bash
npm ci
npm run test:rules
```

**5. Gradle checks**
```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew :app:assembleDebug
./gradlew :app:compileReleaseKotlin   # sanity check del source set release
```

---

## 🧪 Ejecución local equivalente

> Ejecutar en la raíz del repo.

```bash
npm ci
npm run test:rules
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew :app:assembleDebug
./gradlew :app:compileReleaseKotlin
```

---

## 🔒 Seguridad

| Garantía | Detalle |
|---|---|
| ✅ Sin secretos de Firebase | El workflow no usa GitHub Secrets para credenciales. |
| ✅ Sin deploys automáticos | Solo valida build / tests / lint. |
| ✅ Seguro para forks/PRs | No expone infraestructura real. |