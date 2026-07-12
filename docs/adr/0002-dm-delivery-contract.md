# ADR-0002: contrato DM delivery, receipt y seen

- Estado: aceptado
- Fecha: 2026-07-11

## Contexto

FCM send success no demuestra que el dispositivo recibió un mensaje. Además,
un `activeThread` persistido sin freshness podía producir falsos `SEEN`, y el
incremento read-modify-write de unread perdía actualizaciones concurrentes.

## Decisión

- El emisor crea DM en `MSG_DELIVERED` (`1`).
- Solo el servicio Android receptor confirma `MSG_RECEIVED` (`2`).
- `MSG_SEEN` (`3`) requiere UI activa o un `activeThread` coincidente con lease
  de servidor menor o igual a 120 segundos.
- Los estados son monotónicos; Rules impide downgrade y mutación de payload.
- Mensaje y dos resúmenes se escriben en un fan-out raíz atómico.
- Unread del receptor usa incremento de servidor.
- Chat IDs históricos sin underscore conservan `_`; UIDs con underscore usan
  `|` reservado para evitar colisiones entre pares.
- Functions, Android, Rules, tests y documentación comparten el mismo contrato.

## Consecuencias

La semántica distingue entrega de transporte, recepción y lectura real. El
lease reduce falsos seen tras muerte del proceso. El contrato exige tests de
Rules y Functions ante cualquier cambio de path, ownership o estado.
