# ADR-0003: contrato Salas y compatibilidad Groups

## Estado

Aceptado.

## Contexto

El producto conserva datos válidos en `Groups/Meta`, `Groups/Users` y
`Groups/Chat` cuyas keys son nombres visibles. Ese diseño impide renombrar sin
romper paths y mezclaba la sesión local, lectura global y cleanup destructivo
de privados. La navegación actual ya expone Salas y requiere recuperar el valor
del flujo sin restaurar esas limitaciones.

## Decisión

- Las salas nuevas usan una `roomKey` generada e inmutable; `name` es solo
  presentación. `Groups/Names` garantiza unicidad y `Groups/Aliases` reserva una
  key normalizada para alias anónimo o una key técnica estable para perfil real,
  siempre contra el UID autenticado.
- Los paths históricos no se mueven. Cuando falta `roomId`, el path existente
  se adapta como key. Los mensajes y salas válidos siguen siendo legibles.
- Crear, ingresar, cambiar y salir actualizan las ramas relacionadas mediante
  un fan-out raíz. La sesión DataStore se cambia únicamente después del éxito
  remoto. Los reintentos reconcilian una membresía remota ya creada antes de
  persistir la sesión local.
- Dos mensajes distintos pueden compartir el mismo milisegundo de servidor: la
  monotonía se apoya también en `lastMessageId` y el incremento exacto del
  contador, sin rechazar el segundo fan-out.
- La identidad anónima es una proyección pública de la membresía; autorización,
  ownership y `senderUid` siempre usan Firebase Auth.
- El unread público vive en `Users/Data/{uid}/Rooms/{roomKey}`. Solo Chat visible
  marca lectura y publica un `activeThread` con lease. Participantes, Privados y
  Explorar no lo hacen.
- Los privados mantienen `Chats/group_dm` y resúmenes `group_dm`. Salir de una
  sala no los elimina.
- Los mensajes públicos no adoptan checks DM. Functions emite data-messages a
  miembros actuales excepto emisor y sala activa; Android conserva la decisión
  final de permiso y preferencia.

## Consecuencias

Hay compatibilidad de lectura y escritura no destructiva sin una migración
remota previa. Los contadores modernos son exactos para salas creadas por este
contrato. En salas legacy incompletas el conteo se deriva de membresías, el
cursor global sigue siendo fallback y los miembros que ya tienen cursor moderno
reciben fan-out por sala. Publicar Rules y la
Function grupal requiere una operación posterior y no forma parte del PR.
