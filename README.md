# QuickMenu

> 🤖 **Proyecto desarrollado con asistencia de IA.** El código fue escrito en gran parte por IA, bajo la dirección de
> [ccjoshua2813](https://github.com/ccjoshua2813), que diseñó las funciones, tomó las decisiones y probó cada versión
> en el juego. Los commits lo indican con la línea `Assisted-by`.

Menú de **comandos rápidos** para servidores **Paper 26.2**, pensado sobre todo para los **jugadores de Bedrock** (conectados por [Geyser](https://geysermc.org)), al menos para mi servidor. Los jugadores de Bedrock no tienen mods ni atajos de teclado como los de Java, así que QuickMenu les da un botón para abrir sus comandos más usados (vender, comprar, ir a casa…) sin escribirlos. También funciona para jugadores de Java.

## Qué hace

- **Bedrock:** un **formulario nativo** de Geyser con los botones. Los jugadores de Bedrock reciben una **brújula** en la hotbar: al tocarla se abre el menú.
- **Java:** un menú de inventario, que se abre con `/menu`. Por defecto **no se les da nada automático** (ni brújula ni atajo de teclado); el atajo Shift + F existe pero viene desactivado.
- **Botones del servidor** definidos en `config.yml`: nombre, icono, comando, quién lo ejecuta (jugador o consola), permiso y si cierra el menú.
- **Botones personales:** cada jugador puede añadir y quitar los suyos desde el menú; siempre los ejecuta el propio jugador (nunca la consola).
- Permisos por botón, con el mensaje de falta de permiso configurable.

## Uso

| Comando | Qué hace |
|---|---|
| `/menu` (alias `/qm`) | Abre el menú |
| `/menu info` | Diagnóstico: si hay Geyser, si te detecta como Bedrock y si tienes la brújula |
| `/menu reload` | Recarga la configuración (`quickmenu.admin`) |

Permisos: `quickmenu.use` y `quickmenu.personal` (todos por defecto) y `quickmenu.admin` (operadores).

Botones por defecto: **Vender** (`/sellgui`), **Comprar** (`/shop`), **Casa** (`/home`) y **Fijar casa** (`/sethome`). Cámbialos en `plugins/QuickMenu/config.yml`.

## Instalación

1. Descarga `QuickMenu-x.y.z.jar` de las [Releases](../../releases) y ponlo en `plugins/`.
2. Para Bedrock necesitas **Geyser** (y normalmente Floodgate). Sin Geyser el plugin sigue funcionando para Java.
3. Arranca el servidor y edita `plugins/QuickMenu/config.yml`.

## Requisitos y límites

- Paper 26.2 y Java 25. Geyser opcional.
- Los formularios de Bedrock se construyen por reflexión con el cargador de clases de Geyser, para evitar el `LinkageError` que produce otra copia de Cumulus (por ejemplo la de Floodgate).
- Si ya existe un `config.yml`, las opciones nuevas no se añaden solas: cópialas de la versión por defecto.
- Los comandos de los botones (`/home`, `/shop`…) dependen de otros plugins y de sus permisos.

## Numeración

Las versiones empiezan en **0.0.1** y siguen como 0.0.x mientras se prueba. El historial anterior a la primera publicación se resume en el [CHANGELOG](CHANGELOG.md).

## Transparencia sobre el uso de IA

El código fue escrito en gran parte por IA, bajo la dirección de ccjoshua2813, que diseñó las funciones, tomó las decisiones y probó cada versión en el juego. Los commits lo indican con la línea `Assisted-by`.

## Licencia

[MIT](LICENSE).
