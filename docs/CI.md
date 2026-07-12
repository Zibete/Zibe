# CI y matriz de validación

El workflow `.github/workflows/android-ci.yml` valida PRs a `main` sin secretos
ni acceso al backend real. Usa JDK 17, Node 20, configuración Firebase dummy y
no ejecuta deploys.

## Matriz automática

```bash
python scripts/check_architecture.py
python -m py_compile functions/main.py
python -m unittest discover functions/tests
npm ci
npm run test:rules
./gradlew testDebugUnitTest
./gradlew :app:generateDebugAndroidTestLintModel
./gradlew :app:compileDebugAndroidTestKotlin
./gradlew lintDebug
./gradlew :app:assembleDebug
./gradlew :app:compileReleaseKotlin
```

CI publica reportes unitarios y de lint aunque un paso posterior falle. El
grafo Hilt de androidTest se compila explícitamente para detectar bindings de
test incompletos.

## Validación con dispositivo

Instrumentation no corre en GitHub Actions; se ejecuta antes de publicar un PR
de riesgo Android cuando hay dispositivo o emulador disponible:

```bash
adb devices
./gradlew :app:connectedDebugAndroidTest
```

Los flujos con credenciales reales, dos usuarios, push, background/killed,
camera, crop, micrófono y retorno desde Settings requieren checklist física. Un
PR queda draft si esa validación obligatoria no fue completada.

## Seguridad operativa

- `app/google-services.example.json` y valores dummy permiten compilar.
- No se versionan `google-services.json`, `local.properties` ni secretos.
- CI no hace deploy de Functions/Rules, merge, release ni tag.
- Los tests de Rules usan el proyecto demo y Firebase Emulator.
