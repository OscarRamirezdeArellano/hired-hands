# Miner

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
