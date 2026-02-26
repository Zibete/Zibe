# 🤝 Contribuir a ZIBE

Gracias por tu interés en **ZIBE**.

> 📌 Este repositorio se publica como **portfolio / caso de estudio**.  
> ✅ **Issues** están habilitados para feedback, bugs e ideas.  
> 🔒 **Pull Requests externos no se aceptan por defecto** (solo por invitación) para preservar coherencia, calidad y autoría del proyecto.

Antes de participar, leé [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) y [SECURITY.md](SECURITY.md).

---

## 🐛 Issues

Usá los templates disponibles:

- 🐛 **Bug report:** describí el problema, pasos para reproducir y resultado esperado.
- ✨ **Feature request:** explicá el objetivo, la propuesta y alternativas consideradas.

Para acelerar el análisis, incluí: dispositivo / versión de Android, commit/tag donde lo viste, logs relevantes (sin datos sensibles) y capturas o video si aplica.

> 🛡️ **Vulnerabilidades:** no se reportan por Issue público. Usá el canal indicado en [SECURITY.md](SECURITY.md).

---

## 🔀 Pull Requests (solo por invitación)

Si creés que un cambio vale la pena, abrí un **Issue** primero con:

- Qué problema resuelve.
- Alcance del cambio (pequeño y enfocado).
- Cómo validarlo.

Si el cambio encaja con el roadmap y los estándares del repo, puedo implementarlo directamente o invitarte a abrir un PR coordinando alcance y validación.

---

## ✅ Validación local (para aportar feedback útil)

**Mínimo (rápido):**

```bash
./gradlew :app:assembleDebug
./gradlew test
```

**Completo (recomendado si el issue toca build / config / UI):**

```bash
./gradlew testDebugUnitTest lintDebug :app:assembleDebug
```

---

## 🔒 Seguridad (obligatorio)

| Regla | Detalle |
|---|---|
| 🚫 **Nunca publicar** | Tokens, API keys, IDs sensibles, `google-services.json`, `local.properties`, keystores (`*.jks`, `*.keystore`), certificados (`*.pem`, `*.p12`, `*.pfx`). |
| ✅ **Usar siempre plantillas** | `app/google-services.example.json` · `local.properties.example` |

---

## 🧾 Licencia

Al participar, aceptás que cualquier aporte compartido en Issues (texto/código) puede ser usado para mejorar el proyecto y queda bajo la licencia del repositorio: **MIT** (ver [LICENSE](LICENSE)).