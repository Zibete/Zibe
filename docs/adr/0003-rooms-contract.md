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
  key técnica estable para el perfil real del creador. Crear exige identidad
  pública en UI, domain, data y Rules; `creatorUid` y la membresía fundadora
  pública pertenecen al UID autenticado. No se crean propietarios anónimos.
- Ingresar en una sala existente sí admite perfil público o alias anónimo. El
  alias es una proyección de la membresía y nunca reemplaza Firebase Auth como
  identidad de autorización.
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
- Explorar conserva la identidad visual histórica: el host de Main provee el
  fondo ZIBE, la pantalla Compose permanece transparente y las cards reproducen
  glass, glow, jerarquía y densidad de `row_group.xml`. Los formularios de crear
  e ingresar solicitan expansión completa mediante `ZibeBottomSheet`.

## Consecuencias

Hay compatibilidad de lectura y escritura no destructiva sin una migración
remota previa. Los contadores modernos son exactos para salas creadas por este
contrato. En salas legacy incompletas el conteo se deriva de membresías, el
cursor global sigue siendo fallback y los miembros que ya tienen cursor moderno
reciben fan-out por sala. Publicar Rules y la
Function grupal requiere una operación posterior y no forma parte del PR.

El cliente moderno no es compatible con las Rules de `main`: su fan-out de
ingreso agrega `Groups/Aliases` y `Users/Data/{uid}/Rooms`, reemplaza metadata
con contadores coherentes y crea membresía más evento en una única escritura.
La prueba exacta pasa con estas Rules y reproduce `PERMISSION_DENIED` al cargar
las Rules de `main`. No se degrada el cliente a escrituras parciales; la prueba
física exige primero un deploy controlado de `database.rules.json`.
