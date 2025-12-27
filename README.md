## CI / Testing

This project uses GitHub Actions to run:

- `./gradlew testDebugUnitTest`
- `./gradlew lintDebug`
- `./gradlew assembleDebug`

### Lint note (Kotlin 2.x + Compose)

Android Lint currently crashes with Kotlin 2.x metadata for some Compose detectors.
To keep lint enabled in CI, we temporarily disabled:

- `CoroutineCreationDuringComposition`
- `StateFlowValueCalledInComposition`

This is a tooling workaround; no production behavior is affected.
