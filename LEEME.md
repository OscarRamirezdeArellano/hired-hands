# Hired Hands

Mercenarios y trabajadores contratables para Minecraft (NeoForge 26.2). Se pagan con
esmeraldas, te siguen, pelean por ti con el equipo que les des, trabajan de leñador, minero,
granjero o pescador, y se van cuando se les acaba el contrato.

Va en **servidor y en todos los clientes** (mismo jar): añade una entidad nueva.

**Idiomas**: español e inglés. Cada jugador ve los mensajes, estados, saludos y el manual en el
idioma de su juego; si no es español, en inglés. Las órdenes por chat se entienden en los dos
idiomas ("Rodrigo, sígueme" / "Rodrigo, follow me") y la IA contesta en el idioma en que le hablas.
Los textos están en `src/main/resources/assets/hired_hands/lang/` (`en_us.json`, `es_*.json`).

## Cómo se consiguen

- **Mercenarios libres**: aparecen de vez en cuando en biomas con aldeas (llanura, sabana, taiga,
  nieve, desierto), con equipo básico al azar. Clic derecho con esmeraldas en la mano para
  contratarlos (10 esmeraldas = 5 días por defecto). Sin esmeraldas te dicen cuánto cobran.
- **Contrato de mercenario** (se craftea sin forma): papel + espada de hierro + 5 esmeraldas.
  Úsalo sobre el suelo y aparece un mercenario ya contratado por ti.
- **Huevo generador** (creativo): aparece un mercenario libre.

En creativo, todo lo del mod (contrato, manual, cuerno, vara, Entrada de mina y huevo) está en su propia pestaña,
**Hired Hands**, con el contrato como icono. El huevo también sale en la pestaña de huevos.

## Órdenes (clic derecho sobre tu mercenario)

| En la mano | Qué hace |
|---|---|
| Nada | Cambia de orden (ver abajo) |
| Nada + agachado | Abre su **pantalla de gestión** (equipo, mochila, órdenes, grupo) |
| Palo | Te devuelve todo su equipo |
| Esmeralda | +1 día de contrato por esmeralda (agachado: paga todo el montón) |
| Comida (si está herido) | Lo cura |
| Arma o herramienta | Se la equipa, te devuelve la que tenía y **cambia de oficio** (ver abajo) |
| Escudo | A la mano izquierda |
| Armadura | A su ranura; te devuelve la anterior |
| Brotes, semillas, antorchas, piedra | A su mochila, si su oficio las usa |

Cada clic muestra su estado arriba de la barra: oficio, qué está haciendo, días de contrato y vida.

## Oficios

El oficio lo decide lo que tiene en la mano:

| Le das | Oficio |
|---|---|
| Espada, lanza, arco, tridente, maza | **Mercenario** (pelea) |
| Hacha | **Leñador** |
| Pico | **Minero** |
| Azada | **Granjero** |
| Caña de pescar | **Pescador** |

### Órdenes (clic con la mano vacía)

- **Mercenario**: Siguiéndote ↔ De guardia (defiende ese sitio).
- **Trabajador**: Siguiéndote → **Esperando** (quieto ahí, solo se defiende) → **Trabajando**
  (hace su oficio en ese sitio) → Siguiéndote...

Para ponerlo a trabajar: llévalo al sitio, clic (Esperando), clic (Trabajando). Trabaja en un radio
de 16 bloques. Pon un **cofre** (o barril, de Minecraft o de mods) a menos de 5 bloques: ahí deja lo
que saca. En su sitio no sale a cazar monstruos, pero se defiende.

**El cofre de su puesto** también le sirve de despensa y armería:
- **No gasta la herramienta hasta romperla**: con 2 usos le quedan, la cambia por una de repuesto
  (de su mochila o del cofre) y deja la vieja en el cofre para que la repares. Si no hay repuesto,
  te avisa por chat y espera.
- Si está por debajo de la mitad de vida, **come la comida que haya en el cofre**.

**De noche** los trabajadores dejan de trabajar (menos el minero, que está bajo tierra) y **duermen en
una cama** libre a menos de 8 bloques de su puesto; si no hay, esperan en él. Durmiendo se curan el
triple de rápido. Al amanecer vuelven al trabajo.

- **Leñador**: tala como un jugador: alcanza unos 5 bloques y, si el árbol es alto, sube por un
  pilar que va construyendo (saltando y poniendo tierra, piedra o troncos debajo), tala desde arriba
  y baja rompiendo el pilar. Recoge del suelo los brotes, palos y manzanas que caen al pudrirse las
  hojas y replanta con el brote del mismo árbol (si aún no tiene, vuelve a replantar después).
  Deja la madera en el cofre después de cada árbol. Solo tala árboles **naturales**: si las hojas
  las puso un jugador, no lo toca (tus casas de madera están a salvo).
- **Granjero**: cosecha trigo, zanahorias, papas, remolachas, melones, calabazas y caña de azúcar,
  y vuelve a sembrar. Siembra la tierra arada vacía si tiene semillas.
- **Pescador**: busca el **agua abierta** más cercana (primero en su zona, luego hasta 32 bloques),
  se pone en la orilla con la vista despejada, lanza el corcho unos bloques mar adentro y pesca
  con las probabilidades de Minecraft: 85 % peces, 10 % basura y 5 % tesoro si el corcho está en
  aguas abiertas (5x5 de agua alrededor y cielo despejado encima, la misma regla que para un jugador). No pesca agua tapada por la tierra; si el corcho cae fuera
  del agua o no llega a la orilla, busca otro sitio.
- **Minero**, dos formas según dónde lo pongas:
  - **Mina real**: mina como un jugador con experiencia, hacia donde **tú mirabas** al ponerlo a
    trabajar:
    1. **Objetivo** (opcional): dale clic con el mineral (un diamante, hierro crudo, un lingote, la
       mena...) o dile por chat "Rodrigo, busca 10 diamantes". Sin objetivo, mina en la altura de su
       puesto y saca todo lo que encuentre.
    2. **Baja por una escalera** hasta la capa donde más abunda ese mineral: diamante y redstone
       Y −58, oro Y −16, lapislázuli Y 0, hierro Y 16, cobre Y 48, carbón Y 96 (solo baja: si la capa
       está por encima del puesto, mina a la altura del puesto).
    3. **Minería en ramas**: túnel principal de 64 bloques con ramales de 16 a cada lado cada 3
       bloques. Saca todas las vetas que ve (las del objetivo primero). Buscando algo, no pierde el
       tiempo con carbón ni cobre.
    4. **Obstáculos**: tapa con piedra la lava y el agua, pone suelo sobre cuevas y huecos, cambia
       por piedra los techos de arena o grava, rellena los agujeros que deja al sacar menas del suelo,
       y si el camino se le tapa por detrás vuelve a despejarlo. Si algo no se puede romper (roca
       madre, un spawner), deja ese ramal y sigue con el siguiente.
    5. **Piedra**: guarda 64 para tapar cosas; la que sobra no la recoge (no llena la mochila de escombro).
    6. **Cofre de la mina**: si le das un cofre, lo pone al fondo de la escalera y deja ahí lo que saca
       (si no, sube al cofre de su puesto).
    7. **Objetivo cumplido**: te avisa ("¡encontré 10 diamantes!"), lo deja en el cofre y para.

    En una prueba con objetivo "3 diamantes" bajó de Y 64 a Y −58, puso su cofre y encontró los 3
    diamantes (con la velocidad de picado acelerada para la prueba: a velocidad normal tarda bastante más).
  - **Expediciones**: si hay una **Entrada de mina** a menos de 4 bloques de su puesto, no cava:
    entra, desaparece 5 minutos y vuelve con minerales. Lo que trae depende de la **altura de la
    entrada** (por encima de Y 48: carbón, cobre, hierro; entre 0 y 48: además oro, redstone, lapis;
    bajo Y 0: hierro, oro, redstone, lapis y **diamante**) y de su **pico** (con pico de piedra no hay
    diamantes, como en el juego). A veces vuelve herido. No toca el mundo. Después de cada viaje deja
    todo en el cofre y descansa 20 segundos; si está herido (menos del 70 % de vida) espera a curarse.
    Si le das clic mientras está dentro, al salir se queda **Esperando** en vez de volver a entrar.

**Entrada de mina** (con forma): troncos arriba; piedra, pico de hierro, piedra en medio;
piedra, raíl, piedra abajo.

Los trabajadores solo trabajan si el chunk está cargado (hay alguien cerca). Las expediciones
cuentan tiempo del mundo, así que la que esté en curso termina aunque te vayas.

## Comportamiento

- Ataca monstruos cercanos (nunca creepers, endermans ni piglins), a quien te ataca y a lo que tú atacas.
  Con arco dispara desde lejos (flechas infinitas).
- Siguiéndote: no se aleja más de 20 bloques de ti persiguiendo monstruos y se teletransporta si te alejas.
- De guardia: pelea en un radio de 12 bloques de su puesto y luego vuelve.
- Tú y tus otros mercenarios no le hacéis daño.
- Se cura solo fuera de combate. Al morir suelta **siempre** lo que tú le diste.
- Te avisa por chat cuando le queda menos de un día de contrato. Al terminar se queda libre,
  con su equipo, y cualquiera puede volver a contratarlo.
- El contrato cuenta días del mundo: corren aunque no haya nadie conectado.

## Pantalla de gestión, grupos y niveles

- **Pantalla** (agachado + clic con la mano vacía): el mercenario en 3D, su equipo (6 ranuras: puedes
  ponerle y quitarle armadura y herramienta arrastrando), su mochila, nivel y experiencia, vida,
  días de contrato y qué está haciendo. Botones: Seguir, Esperar, Trabajar y Grupo (1 a 4).
- **Grupos**: en el chat, "todos, síganme" / "all, follow me" o "grupo 2, quédense" / "group 2, stay"
  (solo órdenes sencillas). **Cuerno de mando** (huesos ×2, cuero y lingote de oro): clic derecho y
  te siguen todos a 48 bloques; agachado, esperan donde están.
- **Niveles** (1 a 10): suben trabajando (+1 por bloque trabajado, +2 por pez, +10 por expedición)
  y peleando (+5 por monstruo). Cada nivel: +2 de vida, +0,5 de daño, 4 % más rápido trabajando;
  el minero encuentra una veta más por expedición cada 3 niveles.

## Oficios nuevos

| Le das | Oficio | Qué hace |
|---|---|---|
| Tijeras | **Ganadero** | Esquila, cría (con pienso de su mochila o del cofre) hasta 10 animales por especie, sacrifica los que sobran y recoge lana, huevos, carne, cuero... |
| Saco (bundle) | **Cargador** | Lleva al cofre de su puesto lo que hay en los cofres de su zona. No toca los cofres de tus otros trabajadores. |
| Cuenco | **Cocinero** | Carga hornos y ahumadores con comida cruda y carbón del cofre y lleva lo cocinado al cofre. |

## Zona de trabajo

**Vara de capataz** (palo, pepita de oro y tinte rojo): clic derecho en dos bloques para marcar una
zona (máximo 64×64) y luego clic derecho al trabajador: trabaja solo ahí. Agachado sobre él: se la
quitas. Con la vara en la mano ves el borde de la zona de tus trabajadores.

## Aspecto

12 skins propias medievales (túnicas, cinturón, brazales, capuchas, barbas) en vez de las de Steve.

## Manual en el juego

El **Manual del patrón** (libro verde) explica todo esto dentro del juego. Se lo dan a cada jugador
la primera vez que contrata a alguien, y se craftea con un libro y una esmeralda.

## Hablar con ellos

**Saludos**: cuando te acercas (a 6 bloques y a la vista) tu mercenario te saluda en el chat; uno
libre saluda a cualquiera y ofrece sus servicios con su precio. Como mucho un saludo cada 5 minutos
por jugador. No usa IA.

**Órdenes por chat**: escribe el nombre de tu mercenario (el primero, sin apodo) seguido de coma o
dos puntos. Funciona con tus mercenarios a menos de 64 bloques; el mensaje sigue apareciendo en el
chat para todos, como cualquier otro, y la respuesta solo la ves tú.

Las **órdenes sencillas se entienden sin IA** (gratis e instantáneas):

| Dices | Hace |
|---|---|
| sígueme, ven, vamos, acompáñame | Te sigue |
| quédate, espera, quieto, alto | Espera donde está |
| vigila, guardia, defiende | Monta guardia donde estás tú (vuelve a mercenario) |
| a talar / a minar / a cosechar / a pescar | Cambia a ese oficio (busca la herramienta en su mochila o en el cofre) y trabaja donde estás tú |
| busca diamantes / tráeme 10 de hierro / oro | Se hace minero y va a por ese mineral (escalera hasta su capa, hacia donde miras) |
| trabaja, a trabajar | Trabaja en su oficio donde estás tú |
| ¿qué haces?, ¿cómo vas?, informe | Te cuenta qué hace, cuánto lleva en la mochila y cuántos días de contrato le quedan |

Todo lo demás (frases largas, con "no" o "deja", preguntas, charla) va a **Claude**, que entiende la
orden, la cumple y contesta en personaje. Recuerda los últimos 4 intercambios durante 10 minutos, así
que puedes tener una conversación corta. Cada jugador puede mandar hasta 6 mensajes por minuto a la IA.

**Para activar la IA** hay que poner una clave de la API de Anthropic (console.anthropic.com) en el
**servidor**, en `config/hired_hands-common.toml` (sin clave, las órdenes sencillas y los saludos
funcionan igual):

```toml
[ia]
    activada = true
    clave_api = "sk-ant-..."
    modelo = "claude-haiku-4-5"
    llamadas_por_minuto = 6
```

(O la variable de entorno `ANTHROPIC_API_KEY`.) Esta config no se envía a los jugadores.

El modelo por defecto es **Claude Haiku 4.5**, el más barato y rápido: medio centavo de dólar por
mensaje, aproximadamente (1.000 mensajes ≈ 5 dólares). `claude-sonnet-5` o `claude-opus-5`
entienden mejor frases complicadas pero cuestan de 2 a 5 veces más. Con `claude-opus-5`, si el modelo
rechaza una petición, la API la reintenta sola con otro modelo (`fallbacks: "default"`).

Está hecho con llamadas HTTP directas (`java.net.http` + Gson, que ya vienen con Minecraft) en vez
del SDK oficial de Java: NeoForge no deja meter en el jar las librerías del SDK sin nombre de módulo,
y sus dependencias (Kotlin, Jackson) chocarían con las de otros mods del modpack.

## Configuración

`config/hired_hands-server.toml` (se crea al arrancar el servidor, se aplica sin reiniciar):

| Clave | Por defecto | Qué es |
|---|---|---|
| `costo_contratacion` | 10 | Esmeraldas para contratar a un mercenario libre |
| `dias_contrato` | 5 | Días que incluye la contratación o el contrato |
| `dias_por_esmeralda` | 1 | Días extra por cada esmeralda |
| `radio_guardia` | 12 | Radio de combate en modo guardia |
| `radio_trabajo` | 16 | Radio de trabajo alrededor del puesto |
| `lentitud_trabajo` | 2.0 | Tiempo para romper un bloque comparado con un jugador |
| `largo_tunel` | 64 | Largo máximo del túnel del minero |
| `segundos_pesca` | 30 | Tiempo medio entre peces |

Sección `[expedicion]` (minero con Entrada de mina):

| Clave | Por defecto | Qué es |
|---|---|---|
| `duracion_segundos` | 300 | Tiempo dentro de la mina en cada viaje |
| `descanso_segundos` | 20 | Descanso desde que vuelve (incluye dejar las cosas en el cofre) |
| `vida_minima_porcentaje` | 70 | No entra con menos vida; espera a curarse |
| `vetas` | 5 | Vetas que encuentra por viaje: lo que más cambia cuánto trae |
| `probabilidad_herido` | 10 | % de volver herido (pierde 4–10 de vida, nunca muere) |
| `desgaste_pico` | 20 | Usos de pico por viaje (nunca lo rompe: deja al menos 2) |

Receta, frecuencia de aparición y biomas son datos (`src/main/resources/data/hired_hands/`),
modificables con un datapack.

## Compatibilidad

- **Carry On**: los contratados están en su lista negra (`data/carryon/tags/entity_type/entity_blacklist.json`),
  así que agachado + clic siempre abre su pantalla en vez de levantarlos.

## Publicar

En `publish/`: el jar, `changelog-<versión>.md`, `curseforge-description.md` (inglés y español),
`screenshots/` (capturas para CurseForge y la wiki; `_borradores/` son descartes) y `wiki/`
(páginas para la wiki de GitHub). Las imágenes de la descripción y la wiki apuntan a
`github.com/OscarRamirezdeArellano/hired-hands`: funcionan cuando exista ese repositorio.

Las capturas se sacaron en el mundo `run-cliente/saves/Capturas` (copia de New World) con el
datapack `capturas`: la isla está en X 484–555, Z 600–671, Y 64.

## Compilar

Necesita Java 25 (Gradle ya lo descargó en `~/.gradle/jdks`).

```
set JAVA_HOME=C:\Users\oscar\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2
gradlew build
```

El jar sale en `build/libs/`. Sube `mod_version` en `gradle.properties` antes de repartir uno nuevo
y **borra el anterior** de las carpetas `mods`.

## Probar sin tocar el servidor real

- `gradlew runServer`: servidor local en `run/` con RCON (puerto 25575, clave `prueba123`).
  `pause-when-empty-seconds=0` es necesario: si no, el servidor se congela al minuto sin jugadores.
- `gradlew runClient -Pconectar=localhost`: cliente de desarrollo que entra directo a ese servidor.

## Historial

- **0.7.1**: el modo Trabajando se ve como tal ("Leñador · Trabajando: Talando"); arreglos en la
  pantalla de gestión (título de la mochila tapado, botones cortados, contratos de más de 999 días);
  Carry On ya no levanta a los contratados; en el chat la respuesta sale después de tu mensaje y las
  preguntas ya no se toman como órdenes; la receta de la vara de capataz vuelve a cargar y la zona se
  ve por encima de los cultivos. Documentación para publicar: README, CHANGELOG, descripción de
  CurseForge, wiki y capturas en `publish/`.

- **0.7.0**: pantalla de gestión, grupos y cuerno de mando, niveles, oficios de ganadero,
  cargador y cocinero, vara de capataz (zonas), skins propias. Las órdenes sencillas del chat ya no
  dependen de que la IA esté activada.

- **0.6.0 (nombre)**: el mod pasa a llamarse **Hired Hands** (`hired_hands`, paquete `com.oscarways.hiredhands`, licencia MIT)
  para publicarlo en CurseForge. Los mundos de prueba pierden los mercenarios que tenían (cambian los IDs).

- **0.6.0**: en español e inglés (mensajes, manual, órdenes por chat). Pestaña propia en creativo.
  Tesoros al pescar (5 % en aguas abiertas, como un jugador). Sección `[expedicion]` en la config.
- **0.5.0**: minero real rehecho: objetivos por mineral (clic con el mineral o por chat), escalera
  hasta la capa del mineral, minería en ramas, tapa lava y agua, suelo sobre huecos, techos de arena,
  vuelve a despejar si se le tapa el camino, cofre al fondo de la mina. Ninguna herramienta se gasta
  hasta romperse. Pescador con corcho y sedal (animación de pesca); busca agua abierta más lejos y
  no pesca agua tapada por la tierra.
- **0.4.0**: cogen herramientas y comida del cofre; de noche duermen en una cama; Manual del patrón
  (libro en el juego); órdenes por chat: las sencillas sin IA, el resto y la charla con Claude Haiku 4.5
  (recuerda la conversación reciente); saludan al acercarte.
- **0.3.0**: orden nueva "Esperando" (seguir → esperar → trabajar). Leñador con alcance de jugador
  y pilar para árboles altos; deja la madera tras cada árbol. El minero descansa entre expediciones
  y no entra herido. `/data get entity <trabajador> Trabajo` muestra qué está haciendo.
- **0.2.0**: oficios de leñador, minero (túnel y expediciones), granjero y pescador; mochila;
  bloque Entrada de mina. El hacha ya no es arma de mercenario: lo convierte en leñador.
- **0.1.0**: primera versión. Mercenarios libres y contratados, contrato, modos seguir/guardia,
  equipo, arco, aparición natural.
