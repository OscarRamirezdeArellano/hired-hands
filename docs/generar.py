"""Genera las páginas de la wiki (VitePress) en inglés (raíz) y español (es/).

Se edita aquí y se vuelve a correr: python docs/generar.py
"""
import os

RAIZ = os.path.dirname(os.path.abspath(__file__))

EN = {}
ES = {}

# ---------------------------------------------------------------- inicio

EN["index"] = """---
layout: home
hero:
  name: Hired Hands
  text: Mercenaries and workers for hire
  tagline: Pay them in emeralds. They fight for you, or work a trade and bring what they gather to a chest.
  image:
    src: /img/logo.png
    alt: Hired Hands
  actions:
    - theme: brand
      text: Get started
      link: /hiring
    - theme: alt
      text: Trades
      link: /trades
features:
  - icon: ⚔️
    title: Mercenaries
    details: They follow you or guard a spot, fight with the gear you give them and level up.
  - icon: ⚒️
    title: Seven trades
    details: Lumberjack, miner, farmer, fisher, rancher, hauler and cook. The tool in their hand decides the job.
  - icon: 📦
    title: A chest at their post
    details: They store what they gather, swap tools before they break and eat when hurt.
  - icon: 💬
    title: Chat orders
    details: "\\"Wren, what are you doing?\\" works without any AI. Optional Claude chat for everything else."
---

![Hired Hands](/img/01-hired-hands.webp)

## Quick start

1. Craft a **Mercenary Contract** (paper + iron sword + 5 emeralds) and use it on the ground.
2. Give the mercenary an **axe**: it becomes a lumberjack.
3. Walk it to a forest, place a **chest** next to it and right-click it twice with an empty hand
   (Waiting, then Working).
4. Come back later for the wood.

## Requirements

- Minecraft **26.2** with **NeoForge** 26.2.0.82 or newer.
- No other mods required.
- Install on the **server and every client**.
"""

ES["index"] = """---
layout: home
hero:
  name: Hired Hands
  text: Mercenarios y trabajadores a sueldo
  tagline: Se pagan con esmeraldas. Pelean por ti, o trabajan un oficio y dejan lo que sacan en un cofre.
  image:
    src: /img/logo.png
    alt: Hired Hands
  actions:
    - theme: brand
      text: Empezar
      link: /es/hiring
    - theme: alt
      text: Oficios
      link: /es/trades
features:
  - icon: ⚔️
    title: Mercenarios
    details: Te siguen o montan guardia, pelean con el equipo que les das y suben de nivel.
  - icon: ⚒️
    title: Siete oficios
    details: Leñador, minero, granjero, pescador, ganadero, cargador y cocinero. La herramienta en su mano decide el trabajo.
  - icon: 📦
    title: Un cofre en su puesto
    details: Guardan lo que sacan, cambian la herramienta antes de romperla y comen si están heridos.
  - icon: 💬
    title: Órdenes por chat
    details: "\\"Wren, ¿qué haces?\\" funciona sin IA. Conversación opcional con Claude para todo lo demás."
---

![Hired Hands](/img/01-hired-hands.webp)

## Para empezar

1. Craftea un **Contrato de mercenario** (papel + espada de hierro + 5 esmeraldas) y úsalo sobre el suelo.
2. Dale un **hacha** al mercenario: se vuelve leñador.
3. Llévalo a un bosque, pon un **cofre** a su lado y dale clic derecho dos veces con la mano vacía
   (Esperando y luego Trabajando).
4. Vuelve más tarde por la madera.

## Requisitos

- Minecraft **26.2** con **NeoForge** 26.2.0.82 o más nuevo.
- No necesita otros mods.
- Se instala en el **servidor y en todos los clientes**.
"""

# ---------------------------------------------------------------- contratar

EN["hiring"] = """# Hiring and contracts

## Getting one

| How | Details |
|---|---|
| Free mercenaries | Appear now and then in village biomes (plains, savanna, taiga, snowy plains, desert) with random basic gear. Right-click with emeralds to hire (10 emeralds = 5 days by default). Without emeralds they tell you their price. |
| Mercenary Contract | Paper + iron sword + 5 emeralds, shapeless. Use it on the ground: a mercenary hired by you appears. |
| Mercenary Spawn Egg | Creative only. Spawns a free mercenary. |

![Contract recipe](/img/40-recipe-contract.png)

The first time you hire someone you also receive the **Patron's Handbook**, a guide to all of this
inside the game.

Every item is in the **Hired Hands** creative tab.

## The contract

- Each emerald you give adds **1 day** (sneak to pay the whole stack).
- Days are world days and keep running while nobody is online.
- With less than a day left, it warns you in chat.
- When it ends it becomes free again, keeps its gear, and anyone can hire it.

## Death

A hired hand always drops everything you gave it, plus its backpack.
"""

ES["hiring"] = """# Contratar

## Cómo se consiguen

| Cómo | Detalles |
|---|---|
| Mercenarios libres | Aparecen de vez en cuando en biomas con aldeas (llanura, sabana, taiga, llanura nevada, desierto) con equipo básico al azar. Clic derecho con esmeraldas para contratarlos (10 esmeraldas = 5 días por defecto). Sin esmeraldas te dicen cuánto cobran. |
| Contrato de mercenario | Papel + espada de hierro + 5 esmeraldas, sin forma. Úsalo sobre el suelo: aparece un mercenario ya contratado por ti. |
| Huevo de mercenario | Solo en creativo. Aparece un mercenario libre. |

![Receta del contrato](/img/40-recipe-contract.png)

La primera vez que contratas a alguien también recibes el **Manual del patrón**, una guía de todo esto
dentro del juego.

Todos los objetos están en la pestaña **Hired Hands** del modo creativo.

## El contrato

- Cada esmeralda que le das suma **1 día** (agachado, pagas todo el montón).
- Son días del mundo: corren aunque no haya nadie conectado.
- Cuando le queda menos de un día, te avisa por chat.
- Al terminar queda libre, se queda con su equipo y cualquiera puede volver a contratarlo.

## Si muere

Siempre suelta todo lo que le diste, y también su mochila.
"""

# ---------------------------------------------------------------- órdenes

EN["orders"] = """# Orders and modes

## Clicking a hired hand

| In your hand | What happens |
|---|---|
| Nothing | Changes mode (see below) |
| Nothing, sneaking | Opens the **management screen** |
| Stick | Gives back all its gear |
| Emerald | +1 contract day (sneak: the whole stack) |
| Food (when hurt) | Heals it |
| Weapon or tool | Equips it, gives back the old one and **changes its trade** |
| Shield | Goes to its off hand |
| Armor | Goes to its slot |
| Saplings, seeds, torches, stone | Into its backpack if its trade uses them |

Each click shows a status line: trade, current task, contract days and health.

## Modes

- **Workers**: Following → **Waiting** (stays put, only defends itself) → **Working** (does its trade
  around that spot) → Following...
- **Mercenaries**: Following ↔ **On guard** (fights within 12 blocks of the spot and comes back).

While following, it never strays more than 20 blocks chasing monsters and teleports to you if you get
far. It attacks monsters (never creepers, endermen or piglins), anyone who attacks you and whatever
you attack. It heals outside combat.

## Management screen

![Management screen](/img/11-management-screen-closeup.png)

- 3D preview that follows the mouse.
- 6 equipment slots: drag armor, weapons and tools in and out.
- 27-slot backpack.
- Trade, level and experience bar, health, contract days and current task.
- Buttons: **Follow**, **Wait**, **Work**, **Group**.

## Groups and the Command Horn

- The **Group** button cycles none → 1 → 2 → 3 → 4.
- In chat: `all, follow me`, `group 2, stay` (simple orders only).
- **Command Horn** (2 bones + leather + gold ingot): right-click and every hired hand within 48 blocks
  follows you; sneak-click and they wait where they are.

![Command Horn recipe](/img/42-recipe-horn.png)

## Levels

Levels go from 1 to 10. Experience comes from work (+1 per block worked, +2 per fish, +10 per
expedition) and combat (+5 per kill).

Each level gives **+2 health**, **+0.5 damage** and **4 % faster work**. Miners find one more vein
per expedition every 3 levels.

![Combat](/img/08-combat.webp)
"""

ES["orders"] = """# Órdenes y modos

## Clic sobre un contratado

| En tu mano | Qué pasa |
|---|---|
| Nada | Cambia de modo (ver abajo) |
| Nada, agachado | Abre la **pantalla de gestión** |
| Palo | Te devuelve todo su equipo |
| Esmeralda | +1 día de contrato (agachado: todo el montón) |
| Comida (si está herido) | Lo cura |
| Arma o herramienta | Se la equipa, te devuelve la anterior y **cambia de oficio** |
| Escudo | A la mano izquierda |
| Armadura | A su ranura |
| Brotes, semillas, antorchas, piedra | A su mochila, si su oficio los usa |

Cada clic muestra una línea de estado: oficio, tarea actual, días de contrato y vida.

## Modos

- **Trabajadores**: Siguiéndote → **Esperando** (quieto ahí, solo se defiende) → **Trabajando** (hace
  su oficio alrededor de ese sitio) → Siguiéndote...
- **Mercenarios**: Siguiéndote ↔ **De guardia** (pelea a 12 bloques del sitio y vuelve).

Cuando te sigue, no se aleja más de 20 bloques persiguiendo monstruos y se teletransporta si te
alejas. Ataca a los monstruos (nunca a creepers, endermans ni piglins), a quien te ataca y a lo que tú
atacas. Se cura solo fuera de combate.

## Pantalla de gestión

![Pantalla de gestión](/img/11-management-screen-closeup.png)

- Vista en 3D que sigue al ratón.
- 6 ranuras de equipo: pon y quita armadura, armas y herramientas arrastrando.
- Mochila de 27 espacios.
- Oficio, nivel y barra de experiencia, vida, días de contrato y tarea actual.
- Botones: **Seguir**, **Esperar**, **Trabajar**, **Grupo**.

## Grupos y el Cuerno de mando

- El botón **Grupo** pasa por ninguno → 1 → 2 → 3 → 4.
- En el chat: `todos, síganme`, `grupo 2, quédense` (solo órdenes sencillas).
- **Cuerno de mando** (2 huesos + cuero + lingote de oro): clic derecho y te siguen todos los
  contratados a 48 bloques; agachado, esperan donde están.

![Receta del cuerno](/img/42-recipe-horn.png)

## Niveles

Van del 1 al 10. La experiencia sale del trabajo (+1 por bloque trabajado, +2 por pez, +10 por
expedición) y del combate (+5 por monstruo).

Cada nivel da **+2 de vida**, **+0,5 de daño** y **4 % más de rapidez** trabajando. El minero
encuentra una veta más por expedición cada 3 niveles.

![Combate](/img/08-combat.webp)
"""

# ---------------------------------------------------------------- oficios

EN["trades"] = """# Trades

The item in its main hand decides the trade:

| Give it | Trade |
|---|---|
| Sword, spear, bow, trident, mace | **Mercenary** (fights) |
| Axe | **Lumberjack** |
| Pickaxe | **Miner** |
| Hoe | **Farmer** |
| Fishing rod | **Fisher** |
| Shears | **Rancher** |
| Bundle | **Hauler** |
| Bowl | **Cook** |

Put a worker in **Working** mode and place a **chest or barrel** (vanilla or modded) within 5 blocks
of that spot. It works within 16 blocks, or inside the area you give it with the
[Foreman's Rod](./work-areas).

## The chest at its post

- **Storage**: everything it gathers goes there.
- **Armory**: it never breaks a tool. With 2 uses left it swaps to a spare from its backpack or the
  chest and leaves the worn one there for you to repair. Without a spare it tells you and waits.
- **Pantry**: below half health it eats food from the chest.

## Night

Workers stop at night (miners underground don't) and sleep in a free bed within 8 blocks of their
post, healing three times faster. Without a bed they wait at their post.

## Lumberjack

![Lumberjack](/img/02-lumberjack.webp)

Chops like a player: about 5 blocks of reach. For tall trees it pillars up with dirt, stone or logs,
chops from the top and removes the pillar on the way down. It picks up saplings, sticks and apples,
replants with the same sapling and stores the wood after each tree.

::: tip Your houses are safe
It only chops **natural** trees. If a player placed the leaves, it leaves them alone.
:::

## Farmer

![Farmer](/img/03-farmer.webp)

Harvests and replants wheat, carrots, potatoes, beetroot, melons, pumpkins and sugar cane, and plants
empty farmland if it has seeds.

## Fisher

![Fisher](/img/09-fisher.webp)

Looks for the nearest **open water** (in its area first, then up to 32 blocks), stands on the shore
with a clear view and casts a real bobber. It uses the vanilla loot tables: 85 % fish, 10 % junk and
5 % treasure when the bobber is in open water, the same rule as for players.

## Rancher

![Rancher](/img/05-rancher.webp)

Shears sheep, breeds animals with feed from its backpack or chest (wheat, carrots, seeds) up to 10 per
species, culls the extras and collects wool, eggs, meat and leather.

## Hauler

![Hauler](/img/07-hauler.webp)

Collects everything from the chests and barrels in its area and brings it to the chest at its post.
It never touches chests within 5 blocks of its own post or of your other workers' posts.

## Cook

![Cook](/img/06-cook.webp)

Loads furnaces and smokers in its area with raw food and fuel (coal, logs, planks, sticks) from the
chest, and brings the cooked food back.

## Miner

See [Miner](./miner).
"""

ES["trades"] = """# Oficios

Lo que tiene en la mano decide el oficio:

| Le das | Oficio |
|---|---|
| Espada, lanza, arco, tridente, maza | **Mercenario** (pelea) |
| Hacha | **Leñador** |
| Pico | **Minero** |
| Azada | **Granjero** |
| Caña de pescar | **Pescador** |
| Tijeras | **Ganadero** |
| Saco (bundle) | **Cargador** |
| Cuenco | **Cocinero** |

Ponlo en modo **Trabajando** y deja un **cofre o barril** (de Minecraft o de mods) a menos de 5
bloques de ese sitio. Trabaja en un radio de 16 bloques, o dentro de la zona que le marques con la
[Vara de capataz](./work-areas).

## El cofre de su puesto

- **Almacén**: ahí deja todo lo que saca.
- **Armería**: nunca rompe una herramienta. Cuando le quedan 2 usos la cambia por una de repuesto de
  su mochila o del cofre y deja la gastada ahí para que la repares. Sin repuesto, te avisa y espera.
- **Despensa**: con menos de la mitad de vida, come la comida del cofre.

## De noche

Los trabajadores paran de noche (menos el minero bajo tierra) y duermen en una cama libre a menos de
8 bloques de su puesto, donde se curan el triple de rápido. Sin cama, esperan en su puesto.

## Leñador

![Leñador](/img/02-lumberjack.webp)

Tala como un jugador: alcanza unos 5 bloques. Para los árboles altos sube por un pilar de tierra,
piedra o troncos, tala desde arriba y quita el pilar al bajar. Recoge brotes, palos y manzanas,
replanta con el brote del mismo árbol y deja la madera en el cofre después de cada árbol.

::: tip Tus casas están a salvo
Solo tala árboles **naturales**. Si las hojas las puso un jugador, no los toca.
:::

## Granjero

![Granjero](/img/03-farmer.webp)

Cosecha y vuelve a sembrar trigo, zanahorias, papas, remolachas, melones, calabazas y caña de azúcar,
y siembra la tierra arada vacía si tiene semillas.

## Pescador

![Pescador](/img/09-fisher.webp)

Busca el **agua abierta** más cercana (primero en su zona y luego hasta 32 bloques), se pone en la
orilla con la vista despejada y lanza un corcho de verdad. Usa las tablas de Minecraft: 85 % peces,
10 % basura y 5 % tesoro si el corcho está en aguas abiertas, la misma regla que para un jugador.

## Ganadero

![Ganadero](/img/05-rancher.webp)

Esquila ovejas, cría animales con pienso de su mochila o del cofre (trigo, zanahorias, semillas) hasta
10 por especie, sacrifica los que sobran y recoge lana, huevos, carne y cuero.

## Cargador

![Cargador](/img/07-hauler.webp)

Recoge todo lo que hay en los cofres y barriles de su zona y lo lleva al cofre de su puesto. Nunca toca
los cofres a menos de 5 bloques de su puesto ni de los puestos de tus otros trabajadores.

## Cocinero

![Cocinero](/img/06-cook.webp)

Carga los hornos y ahumadores de su zona con comida cruda y combustible (carbón, troncos, tablas,
palos) del cofre, y lleva lo cocinado de vuelta.

## Minero

Ver [Minero](./miner).
"""

# ---------------------------------------------------------------- minero

EN["miner"] = """# Miner

The miner works in one of two ways, depending on whether there is a **Mine Entrance** near its post.

## Real mine

![Miner cutaway](/img/04-miner-cutaway.webp)

*Cutaway: the rock above the mine was removed for the screenshot.*

It digs toward the direction **you** were facing when you set it to work.

1. **Target** (optional): click it with the ore, raw ore or ingot, or say `Garrick, find 10 diamonds`.
   Without a target it mines at its post's height and keeps everything.
2. **Staircase** down to the best layer for that ore:

   | Ore | Layer |
   |---|---|
   | Diamond, redstone | Y −58 |
   | Gold | Y −16 |
   | Lapis | Y 0 |
   | Iron | Y 16 |
   | Copper | Y 48 |
   | Coal | Y 96 |

   It only goes down: if the layer is above its post, it mines at the post's height.
3. **Branch mining**: a 64-block main tunnel with 16-block side branches every 3 blocks. It mines every
   vein it sees (the target first) and skips coal and copper while looking for something else.
4. **Obstacles**: it seals lava and water, floors over caves, replaces sand and gravel ceilings, fills
   holes left by ores in the floor and clears the way back if it gets blocked. Unbreakable blocks
   (bedrock, spawners) make it skip that branch.
5. **Stone**: it keeps 64 for patching and leaves the rest.
6. **Mine chest**: give it a chest and it places it at the bottom of the staircase.
7. **Done**: it tells you when it reaches its target, stores the ore and stops.

## Expeditions

With a **Mine Entrance** within 4 blocks of its post it doesn't dig. It goes in, disappears for 5
minutes and comes back with ores. The world is not touched.

- **Loot** depends on the entrance's height (above Y 48: coal, copper, iron; Y 0–48: plus gold,
  redstone and lapis; below Y 0: iron, gold, redstone, lapis and diamond) and on its pickaxe (no
  diamonds with a stone pickaxe).
- Higher levels find more veins.
- It may come back hurt (it never dies there) and won't go back in below 70 % health.
- Click it while it's inside and it will wait at its post when it comes out.

![Mine Entrance recipe](/img/44-recipe-mine-entrance.png)
"""

ES["miner"] = """# Minero

El minero trabaja de dos formas, según haya o no una **Entrada de mina** cerca de su puesto.

## Mina real

![Corte de la mina](/img/04-miner-cutaway.webp)

*Corte: se quitó la roca de encima de la mina para la captura.*

Cava hacia donde **tú** mirabas al ponerlo a trabajar.

1. **Objetivo** (opcional): dale clic con la mena, el mineral crudo o el lingote, o dile
   `Garrick, busca 10 diamantes`. Sin objetivo, mina a la altura de su puesto y se queda con todo.
2. **Escalera** hasta la capa donde más abunda ese mineral:

   | Mineral | Capa |
   |---|---|
   | Diamante, redstone | Y −58 |
   | Oro | Y −16 |
   | Lapislázuli | Y 0 |
   | Hierro | Y 16 |
   | Cobre | Y 48 |
   | Carbón | Y 96 |

   Solo baja: si la capa está por encima del puesto, mina a la altura del puesto.
3. **Minería en ramas**: túnel principal de 64 bloques con ramales de 16 a cada lado cada 3 bloques.
   Saca todas las vetas que ve (primero las del objetivo) y, si busca algo, no pierde tiempo con carbón
   ni cobre.
4. **Obstáculos**: tapa la lava y el agua, pone suelo sobre cuevas, cambia los techos de arena o grava,
   rellena los agujeros que dejan las menas del suelo y vuelve a despejar el camino si se le tapa. Si
   algo no se puede romper (roca madre, un spawner), deja ese ramal.
5. **Piedra**: guarda 64 para tapar cosas y deja el resto.
6. **Cofre de la mina**: si le das un cofre, lo pone al fondo de la escalera.
7. **Objetivo cumplido**: te avisa, deja el mineral en el cofre y para.

## Expediciones

Con una **Entrada de mina** a menos de 4 bloques de su puesto, no cava: entra, desaparece 5 minutos y
vuelve con minerales. No toca el mundo.

- **Lo que trae** depende de la altura de la entrada (por encima de Y 48: carbón, cobre, hierro; de
  Y 0 a 48: además oro, redstone y lapislázuli; bajo Y 0: hierro, oro, redstone, lapislázuli y
  diamante) y de su pico (con pico de piedra no hay diamantes).
- A más nivel, más vetas.
- A veces vuelve herido (nunca muere ahí) y no vuelve a entrar con menos del 70 % de vida.
- Si le das clic mientras está dentro, al salir se queda esperando en su puesto.

![Receta de la entrada de mina](/img/44-recipe-mine-entrance.png)
"""

# ---------------------------------------------------------------- zonas

EN["work-areas"] = """# Work areas

![Work area](/img/13-work-area.webp)

By default a worker works within 16 blocks of its post. The **Foreman's Rod** gives it an exact area.

1. Right-click a block: first corner.
2. Right-click another block: opposite corner (up to 64×64).
3. Right-click the worker: it now works only inside that area.

Sneak-click a worker with the rod to clear its area. While holding the rod you see the outline of your
workers' areas.

![Foreman's Rod recipe](/img/43-recipe-rod.png)
"""

ES["work-areas"] = """# Zonas de trabajo

![Zona de trabajo](/img/13-work-area.webp)

Por defecto un trabajador trabaja a 16 bloques de su puesto. La **Vara de capataz** le da una zona
exacta.

1. Clic derecho en un bloque: primera esquina.
2. Clic derecho en otro bloque: esquina opuesta (hasta 64×64).
3. Clic derecho al trabajador: desde ahora trabaja solo dentro de esa zona.

Agachado y con la vara, dale clic para quitarle la zona. Con la vara en la mano ves el borde de las
zonas de tus trabajadores.

![Receta de la vara](/img/43-recipe-rod.png)
"""

# ---------------------------------------------------------------- chat

EN["chat"] = """# Chat and AI

![Chat](/img/12-chat.webp)

## Greetings

When you come within 6 blocks (and in sight) your hired hand greets you in chat. Free mercenaries
greet anyone and offer their services with their price. At most one greeting every 5 minutes per
player. No AI involved.

## Chat orders

Start the message with its **first name** followed by a comma or colon: `Wren, follow me`. It works
with your hired hands within 64 blocks. Everyone sees your message as usual; only you see the reply.

Simple orders work **without any AI**, instantly:

| You say | It does |
|---|---|
| follow me, come, let's go | Follows you |
| stay, wait, hold, halt | Waits where it is |
| guard, defend, protect | Guards the spot where you are (becomes a mercenary) |
| chop, mine, dig, harvest, farm, fish, shear, haul, cook | Switches to that trade (taking the tool from its backpack or chest) and works where you are |
| find diamonds, find 10 iron | Becomes a miner and goes after that ore |
| work | Works its trade where you are |
| what are you doing?, how are you?, status, report | Tells you what it's doing, what it carries and its contract days |

The same orders work in Spanish (`sígueme`, `quédate`, `a talar`, `busca diamantes`, `¿qué haces?`).

Group orders: `all, <order>` or `group 2, <order>` (simple orders only).

## Optional AI

Anything else (long sentences, negations, questions, small talk) can be sent to **Claude**. It
understands the order, carries it out with the same actions as the simple orders and replies in
character. It remembers the last 4 exchanges for 10 minutes and answers in the language you use.

::: info Off by default
The AI only works if the server owner adds an Anthropic API key (see
[Configuration](./configuration)). The key stays on the server. The default model, Claude Haiku 4.5,
is the cheapest, and each player can send up to 6 AI messages per minute.
:::
"""

ES["chat"] = """# Chat e IA

![Chat](/img/12-chat.webp)

## Saludos

Cuando te acercas (a 6 bloques y a la vista) tu contratado te saluda en el chat. Los mercenarios
libres saludan a cualquiera y ofrecen sus servicios con su precio. Como mucho un saludo cada 5 minutos
por jugador. No usa IA.

## Órdenes por chat

Empieza el mensaje con su **primer nombre** seguido de coma o dos puntos: `Wren, sígueme`. Funciona
con tus contratados a menos de 64 bloques. Todos ven tu mensaje como siempre; la respuesta solo la ves
tú.

Las órdenes sencillas funcionan **sin IA**, al instante:

| Dices | Hace |
|---|---|
| sígueme, ven, vamos, acompáñame | Te sigue |
| quédate, espera, quieto, alto | Espera donde está |
| vigila, guardia, defiende, protege | Monta guardia donde estás tú (vuelve a mercenario) |
| a talar, a minar, a cosechar, a pescar, esquila, carga, cocina | Cambia a ese oficio (busca la herramienta en su mochila o en el cofre) y trabaja donde estás tú |
| busca diamantes, tráeme 10 de hierro | Se hace minero y va por ese mineral |
| trabaja, a trabajar | Trabaja en su oficio donde estás tú |
| ¿qué haces?, ¿cómo vas?, informe | Te cuenta qué hace, qué lleva y cuántos días de contrato le quedan |

Las mismas órdenes funcionan en inglés (`follow me`, `stay`, `chop`, `find diamonds`,
`what are you doing?`).

Órdenes a varios: `todos, <orden>` o `grupo 2, <orden>` (solo órdenes sencillas).

## IA opcional

Todo lo demás (frases largas, con "no", preguntas, charla) puede ir a **Claude**, que entiende la
orden, la cumple con las mismas acciones que las órdenes sencillas y contesta en personaje. Recuerda
los últimos 4 intercambios durante 10 minutos y responde en el idioma en que le hablas.

::: info Desactivada por defecto
La IA solo funciona si el dueño del servidor pone una clave de la API de Anthropic (ver
[Configuración](./configuration)). La clave se queda en el servidor. El modelo por defecto, Claude
Haiku 4.5, es el más barato, y cada jugador puede mandar hasta 6 mensajes por minuto a la IA.
:::
"""

# ---------------------------------------------------------------- configuración

TABLA_EN = """| Key | Default | Meaning |
|---|---|---|
| `costo_contratacion` | 10 | Emeralds to hire a free mercenary |
| `dias_contrato` | 5 | Days included when hiring or using a contract |
| `dias_por_esmeralda` | 1 | Extra days per emerald |
| `radio_guardia` | 12 | Fighting radius around a guard post |
| `radio_trabajo` | 16 | Work radius around a post (without a work area) |
| `lentitud_trabajo` | 2.0 | Time to break a block compared to a player |
| `largo_tunel` | 64 | Length of the miner's main tunnel |
| `segundos_pesca` | 30 | Average seconds between fish |
| `ganado_maximo` | 10 | Animals per species the rancher keeps |"""

EXP_EN = """| Key | Default | Meaning |
|---|---|---|
| `duracion_segundos` | 300 | Time inside the mine per trip |
| `descanso_segundos` | 20 | Rest after coming back |
| `vida_minima_porcentaje` | 70 | Won't go in with less health |
| `vetas` | 5 | Veins found per trip |
| `probabilidad_herido` | 10 | % chance of coming back hurt |
| `desgaste_pico` | 20 | Pickaxe uses per trip (never breaks it) |"""

IA = """```toml
[ia]
    activada = true
    clave_api = "sk-ant-..."
    modelo = "claude-haiku-4-5"
    llamadas_por_minuto = 6
```"""

EN["configuration"] = f"""# Configuration

## `config/hired_hands-server.toml`

Created on first server start. Changes apply without a restart.

{TABLA_EN}

### `[expedicion]`

{EXP_EN}

## AI: `config/hired_hands-common.toml`

Only the **server** needs this file, and it is never sent to players.

{IA}

The key can also come from the `ANTHROPIC_API_KEY` environment variable. Without a key, greetings and
simple orders work exactly the same.

## Datapacks

Recipes, spawn weights and spawn biomes are data files under `data/hired_hands/` and can be
overridden with a datapack.
"""

ES["configuration"] = f"""# Configuración

## `config/hired_hands-server.toml`

Se crea al arrancar el servidor. Los cambios se aplican sin reiniciar.

| Clave | Por defecto | Qué es |
|---|---|---|
| `costo_contratacion` | 10 | Esmeraldas para contratar a un mercenario libre |
| `dias_contrato` | 5 | Días que incluye la contratación o el contrato |
| `dias_por_esmeralda` | 1 | Días extra por esmeralda |
| `radio_guardia` | 12 | Radio de combate alrededor de un puesto de guardia |
| `radio_trabajo` | 16 | Radio de trabajo alrededor del puesto (sin zona de trabajo) |
| `lentitud_trabajo` | 2.0 | Tiempo para romper un bloque comparado con un jugador |
| `largo_tunel` | 64 | Largo del túnel principal del minero |
| `segundos_pesca` | 30 | Segundos promedio entre peces |
| `ganado_maximo` | 10 | Animales por especie que conserva el ganadero |

### `[expedicion]`

| Clave | Por defecto | Qué es |
|---|---|---|
| `duracion_segundos` | 300 | Tiempo dentro de la mina en cada viaje |
| `descanso_segundos` | 20 | Descanso al volver |
| `vida_minima_porcentaje` | 70 | No entra con menos vida |
| `vetas` | 5 | Vetas que encuentra por viaje |
| `probabilidad_herido` | 10 | % de volver herido |
| `desgaste_pico` | 20 | Usos de pico por viaje (nunca lo rompe) |

## IA: `config/hired_hands-common.toml`

Solo lo necesita el **servidor**, y nunca se envía a los jugadores.

{IA}

La clave también puede venir de la variable de entorno `ANTHROPIC_API_KEY`. Sin clave, los saludos y
las órdenes sencillas funcionan igual.

## Datapacks

Las recetas, la frecuencia de aparición y los biomas son archivos de datos en `data/hired_hands/` y se
pueden cambiar con un datapack.
"""

# ---------------------------------------------------------------- recetas

EN["recipes"] = """# Recipes

| Item | Recipe |
|---|---|
| Mercenary Contract | ![](/img/40-recipe-contract.png) |
| Patron's Handbook | ![](/img/41-recipe-manual.png) |
| Command Horn | ![](/img/42-recipe-horn.png) |
| Foreman's Rod | ![](/img/43-recipe-rod.png) |
| Mine Entrance | ![](/img/44-recipe-mine-entrance.png) |

## The Patron's Handbook

Every new patron receives this guide in game.

<div class="libros">

![](/img/20-manual-cover.png)
![](/img/23-manual-trades.png)
![](/img/26-manual-levels.png)
![](/img/27-manual-groups.png)

</div>
"""

ES["recipes"] = """# Recetas

| Objeto | Receta |
|---|---|
| Contrato de mercenario | ![](/img/40-recipe-contract.png) |
| Manual del patrón | ![](/img/41-recipe-manual.png) |
| Cuerno de mando | ![](/img/42-recipe-horn.png) |
| Vara de capataz | ![](/img/43-recipe-rod.png) |
| Entrada de mina | ![](/img/44-recipe-mine-entrance.png) |

## El Manual del patrón

Cada patrón nuevo recibe esta guía en el juego. En un juego en español se ve en español; estas
capturas son de la versión en inglés.

<div class="libros">

![](/img/20-manual-cover.png)
![](/img/23-manual-trades.png)
![](/img/26-manual-levels.png)
![](/img/27-manual-groups.png)

</div>
"""

# ---------------------------------------------------------------- FAQ

EN["faq"] = """# FAQ

### Does it go on the server, the client or both?

Both. It adds a new entity, items and a screen.

### Do I need the AI?

No. Greetings and simple orders work without it. The AI only adds free-form chat.

### My worker says "Waiting" and does nothing.

Click it once more with an empty hand: the third mode is **Working**. A mercenary with a sword has no
Working mode; give it a tool.

### My worker stopped and asks for a tool.

Its tool was about to break. Put a new one in the chest at its post, or repair the old one it left
there.

### Can Carry On pick them up?

No. Hired hands are on Carry On's blacklist, so sneak + right-click always opens their screen.

### Do they work while I'm away?

Only while their chunk is loaded. Contracts and expeditions count world time, so they keep running.

### My fisher doesn't fish.

It needs open water (not covered by blocks) within 32 blocks and a clear view of it from the shore.

### Where do I report a bug?

On [GitHub](https://github.com/OscarRamirezdeArellano/hired-hands/issues).
"""

ES["faq"] = """# Preguntas frecuentes

### ¿Va en el servidor, en el cliente o en los dos?

En los dos. Añade una entidad, objetos y una pantalla.

### ¿Necesito la IA?

No. Los saludos y las órdenes sencillas funcionan sin ella. La IA solo añade conversación libre.

### Mi trabajador dice "Esperando" y no hace nada.

Dale otro clic con la mano vacía: el tercer modo es **Trabajando**. Un mercenario con espada no tiene
modo Trabajando; dale una herramienta.

### Mi trabajador paró y pide una herramienta.

Se le iba a romper. Deja una nueva en el cofre de su puesto, o repara la vieja que dejó ahí.

### ¿Carry On puede levantarlos?

No. Están en la lista negra de Carry On, así que agachado + clic derecho siempre abre su pantalla.

### ¿Trabajan cuando no estoy?

Solo si su chunk está cargado. Los contratos y las expediciones cuentan tiempo del mundo, así que
siguen corriendo.

### Mi pescador no pesca.

Necesita agua abierta (no tapada por bloques) a menos de 32 bloques y verla despejada desde la orilla.

### ¿Dónde reporto un error?

En [GitHub](https://github.com/OscarRamirezdeArellano/hired-hands/issues).
"""

for nombre, texto in EN.items():
    with open(os.path.join(RAIZ, nombre + ".md"), "w", encoding="utf-8") as f:
        f.write(texto)
os.makedirs(os.path.join(RAIZ, "es"), exist_ok=True)
for nombre, texto in ES.items():
    with open(os.path.join(RAIZ, "es", nombre + ".md"), "w", encoding="utf-8") as f:
        f.write(texto)
print(len(EN), "páginas en inglés,", len(ES), "en español")
