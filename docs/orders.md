# Orders and modes

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
