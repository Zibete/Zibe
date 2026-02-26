# 🛡️ Política de seguridad

Este repositorio se publica como **portfolio técnico**. La seguridad se gestiona en modalidad **mejor esfuerzo** (sin SLA ni bug bounty).

---

## 🚨 Cómo reportar una vulnerabilidad

| Opción | Cuándo usarla |
|---|---|
| ✅ **GitHub Security Advisories** *(preferido)* | Siempre que esté disponible — reporte privado. |
| ✅ **Issue sin detalles técnicos** | Si Advisories no estuviera disponible — solicitá un canal privado. |

> 🚫 No publiques PoCs, endpoints, credenciales, tokens ni pasos explotables en Issues o PRs.

---

## 🧾 Qué incluir en el reporte

- 📌 Componente afectado (módulo / feature / archivo).
- 🧪 Pasos mínimos para reproducir (sin exponer datos sensibles).
- 🎯 Impacto: qué se puede hacer / qué se expone.
- 🧾 Versión / commit / tag donde lo viste.
- 📷 Logs o capturas si ayudan (redactando datos sensibles).

---

## ✅ Alcance

**En alcance:**
- Código del repo (app, módulos, utilidades, configuraciones versionadas).
- Workflows de CI (`.github/workflows/*`).
- Dependencias y configuraciones de build con riesgo potencial.

**Fuera de alcance:**
- Configuración de Firebase propia del usuario (proyectos externos al clonar).
- Problemas derivados de credenciales locales o archivos ignorados (`local.properties`, `google-services.json`, keystores).
- Bugs funcionales sin impacto de seguridad → usar Issues normales.

---

## 🔒 Manejo de secretos

| Regla | Detalle |
|---|---|
| 🚫 **Nunca versionar** | `google-services.json`, `local.properties`, keystores (`*.jks`, `*.keystore`), llaves/certs (`*.pem`, `*.p12`, `*.pfx`). |
| ✅ **Usar siempre plantillas** | `app/google-services.example.json` · `local.properties.example` |
| ⚠️ **Si se publicó un secreto por error** | Reportar por canal privado → asumir compromiso (rotar/revocar) → documentar el fix. |

---

## 🧰 Buenas prácticas recomendadas

- Mantener activos: **Secret scanning**, **Dependabot alerts** y **Code scanning** (si aplica).
- Revisar PRs con foco en: credenciales hardcodeadas, cambios en rules/config, permisos sensibles y tráfico no cifrado.

---

## 🤝 Divulgación responsable

Agradezco reportes responsables. Evitá técnicas destructivas o que comprometan datos reales.