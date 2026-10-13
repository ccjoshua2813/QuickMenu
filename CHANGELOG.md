# QuickMenu — historial de versiones

Plugin para Paper 26.2 (compilado contra build 119) pensado para jugadores de Bedrock vía Geyser. La numeración empieza en 0.0.1 con la primera publicación en GitHub.

## 0.0.1
Primera publicación en GitHub. Reúne todo lo desarrollado y probado hasta ahora (antes con numeración interna 0.1.0–0.3.0, ver más abajo):
- Menú de comandos rápidos con `/menu` (alias `/qm`): **formulario nativo para Bedrock** (Geyser) y menú de inventario para Java. Los formularios se construyen por reflexión con el cargador de clases de Geyser para evitar conflictos con otras copias de Cumulus (Floodgate).
- Botones del servidor en `config.yml` (nombre, icono, comando, quién lo ejecuta, permiso y si cierra el menú) y botones personales que cada jugador añade y quita.
- Brújula que abre el menú, entregada **solo a jugadores de Bedrock** por defecto; los de Java no reciben nada automático (el atajo Shift+F viene desactivado).
- Botones por defecto: Vender, Comprar, Casa y Fijar casa.
- Permisos: `quickmenu.use` y `quickmenu.personal` (todos) y `quickmenu.admin` (operadores).

## Historial interno (numeración anterior, antes de publicar)
## 0.3.0
- Los jugadores de Java ya no reciben nada automático: ni brújula (solo Bedrock) ni el atajo Shift+F, que ahora viene desactivado (`open-key.swap-hand: false`). Siguen pudiendo abrir el menú con `/menu`.
- Botón nuevo por defecto: Fijar casa (`/sethome`), junto a Casa. Solo afecta a configuraciones nuevas.

## 0.2.0
- La brújula del menú ahora se entrega solo a jugadores de Bedrock (`open-item.only-bedrock: true`, por defecto). A los de Java se les quita si ya la tenían. Con `only-bedrock: false` vuelve a repartirse a todos.
- Nuevo atajo para Java sin item: **Shift + F** (cambiar de mano agachado) abre el menú. Se configura con `open-key.swap-hand` y `open-key.require-sneak`.
- Al entrar, el reparto de la brújula espera un segundo para que Geyser tenga registrada la conexión de Bedrock.
- Las opciones nuevas funcionan aunque el `config.yml` existente no las tenga (usan sus valores por defecto).
- Botones por defecto: Vender (`/sellgui`), Comprar (`/shop`) y Casa (`/home`), con colores oscuros que se leen mejor sobre el gris de los formularios de Bedrock. El ejemplo de moderador queda comentado. Solo afecta a configuraciones nuevas.

## 0.1.3
- Arreglo definitivo del `LinkageError` de los formularios de Bedrock: aislar el plugin (0.1.2) no bastó, porque un plugin de Paper que une el classpath de Geyser sigue viendo las clases de los demás plugins clásicos. Ahora los formularios se construyen y envían por reflexión usando el cargador de clases de Geyser (`GeyserForms`), de modo que nunca se mezclan dos copias de Cumulus.
- Comprobado en local que los tres tipos de formulario (lista de botones, cuadros de texto y lista desplegable) se construyen con las clases reales de Cumulus. Pendiente de probar en un servidor con Geyser.

## 0.1.2
- Arreglo del `LinkageError` con los formularios de Bedrock: el servidor tenía dos copias de la clase `Form` de Cumulus (Geyser y otro plugin, probablemente Floodgate) y el plugin tomaba la equivocada. Ahora es un plugin de Paper (`paper-plugin.yml`) con classpath aislado que solo ve las clases de Geyser.
- El comando `/menu` y los permisos ahora se registran por código (un plugin de Paper no puede declararlos en un archivo).

## 0.1.1
- Bedrock: la conexión de Geyser se busca también por nombre y UUID de Java (antes solo por UUID, y si fallaba no pasaba nada), y los fallos al enviar el formulario ahora se avisan en la consola.
- El menú se abre 3 ticks después de tocar la brújula (Bedrock ignora formularios que llegan en el mismo instante del toque).
- Nuevo `/menu info`: muestra si el servidor tiene Geyser, si te detecta como Bedrock y si tienes la brújula.
- Nueva opción `debug: true` en `config.yml` para ver en la consola qué hace el plugin.

## 0.1.0
- Menú de comandos rápidos con `/menu` (alias `/qm`) y un item que se entrega al entrar (brújula, clic derecho).
- Jugadores de Java: menú de inventario. Jugadores de Bedrock (Geyser): formulario nativo.
- Botones del servidor definidos en `config.yml`: nombre, icono, comando, quién lo ejecuta (jugador o consola), permiso y si cierra el menú.
- Botones personales: cada jugador puede añadir y quitar los suyos (Java: botón "+ Añadir comando" con preguntas por chat y clic derecho para eliminar; Bedrock: formularios). Máximo configurable, guardados por jugador.
- `/menu reload` para recargar la configuración (permiso `quickmenu.admin`).
