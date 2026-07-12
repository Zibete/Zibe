# ADR-0001: límites modulares y dirección de dependencias

- Estado: aceptado
- Fecha: 2026-07-11

## Contexto

`domain` importaba Android, Firebase y proveedores; presentation recibía
repositorios concretos y referencias RTDB. Eso hacía que tests, lifecycle y
cambios de backend atravesaran toda la aplicación.

## Decisión

`domain` contiene modelos, contratos y casos de uso puros. `data` implementa los
contratos y posee Firebase/DataStore. `app` compone Hilt y concentra Android/UI.
`core:common` y `core:designsystem` son dependencias transversales acotadas.

Las nuevas capacidades se agregan extendiendo la abstracción dueña. El check
`scripts/check_architecture.py` bloquea imports prohibidos y dependencias
concretas desde presentation.

## Consecuencias

- ViewModels y casos de uso pueden probarse sin Firebase ni framework.
- Los modelos que cruzan capas son propios de ZIBE.
- El wiring Hilt y los adapters agregan código explícito, pero evitan fugas de
  infraestructura y duplicación de repositorios.

