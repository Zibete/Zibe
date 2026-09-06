# RoomsV2 — contrato de arquitectura y seguridad

RoomsV2 es un esquema aislado para reemplazar gradualmente la experiencia legacy de `Groups` sin modificar ni migrar producción durante el desarrollo. El corte usa IDs públicos opacos, membresías persistentes y operaciones de escritura privilegiadas en Cloud Functions.

## Objetivos cubiertos en este corte

- Un usuario puede mantener membresías activas en varias salas; seleccionar otra sala no abandona las anteriores.
- Crear sala requiere perfil real. El creador queda como `owner` y miembro activo.
- Ingreso con identidad real o alias anónimo. El alias se normaliza con NFKC + minúsculas + espacios colapsados y se reclama de forma transaccional.
- Una identidad anónima pública nunca contiene UID. El vínculo `uid ↔ identityId` vive únicamente en `RoomsV2/private` y las Rules niegan su lectura al cliente.
- Reingreso de la misma cuenta con el mismo alias recupera el mismo `identityId`; otra cuenta que tome un alias liberado recibe otro `identityId` y no hereda historial.
- Envío de texto usa `clientMessageId` y un claim privado transaccional para que un reintento reutilice el mismo `messageId`.
- Todo write de cliente bajo `RoomsV2` está denegado. Functions valida autenticación, pertenencia, estado de sala, ban y payload antes de escribir.
- El owner puede cerrar la sala, transferir propiedad a una identidad real activa y designar/revocar moderadores. Owner/moderador pueden expulsar o banear respetando jerarquía; un moderador no puede sancionar owner ni otro moderador.

## Árbol RTDB

```text
RoomsV2/
  publicRooms/{roomId}
  publicMembers/{roomId}/{identityId}
  publicMessages/{roomId}/{messageId}
  membershipIndexByUser/{uid}/{roomId}
  private/
    userIdentity/{uid}/{roomId}/{identityKey}
    identityOwners/{roomId}/{identityId}
    aliasClaims/{roomId}/{aliasHash}
    messageClaims/{uid}/{roomId}/{clientMessageId}
    bans/{roomId}/{uid}
```

`publicRooms`, `publicMembers` y `publicMessages` no usan UID como clave ni valor de identidad. `membershipIndexByUser` es legible únicamente por su propietario autenticado. `private` es server-only.

## Escrituras y concurrencia

`create_room_v2`, `join_room_v2`, `leave_room_v2`, `send_room_v2_text`, `close_room_v2`, `transfer_room_owner_v2`, `set_room_moderator_v2`, `kick_room_member_v2` y `ban_room_member_v2` son callables autenticadas. Las proyecciones relacionadas se escriben mediante un único `update()` multipath de RTDB cuando no hace falta leer-modificar-escribir. Firebase documenta esas actualizaciones como atómicas dentro de Realtime Database.

Los claims de alias y de `clientMessageId` usan transacciones. `memberCount` se actualiza con una transacción independiente porque es un contador derivado; no participa de autorización y deberá tener reconciliación antes del cierre de la entrega. No se presenta como atomicidad entre operaciones independientes.

## Separación respecto del legacy

- No se modifica `Groups`, `Chats/dm` ni `Chats/group_dm`.
- `functions/legacy_main.py` conserva byte por byte el entrypoint previo; `functions/main.py` expone las Functions legacy y agrega callables V2 sin cambiar sus paths.
- El repositorio Android V2 es paralelo a `GroupRepository` y todavía no sustituye `GroupsViewModel`/`GroupHost`. Esto evita dejar la UI apuntando a un backend a medio migrar.

## Rules durante desarrollo

`database.roomsv2.rules.json` es un ruleset mínimo independiente para probar la frontera pública/privada sin alterar `database.rules.json` de producción. `firebase.roomsv2.local.json` usa un puerto de Database Emulator separado (`9001`). Antes del corte UI, el fragmento deberá integrarse al ruleset local completo y reejecutarse junto con los 77 tests legacy.

Comando dedicado:

```powershell
npm run test:rules:v2
```

## Pendientes del contrato completo

Este corte todavía no implementa: UI de propiedad/moderación, reportes/evidencia, privados contextuales V2, unread/lectura, presencia por dispositivo, multimedia, Storage authorization, notificaciones V2 y migración de la UI de Salas. Ninguno de esos pendientes debe implementarse sobre paths legacy para “simular” compatibilidad.
